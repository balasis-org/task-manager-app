package io.github.balasis.taskmanager.engine.infrastructure.textanalytics.service;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.models.AnalyzeActionsResult;
import com.azure.ai.textanalytics.models.AnalyzeSentimentAction;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import com.azure.ai.textanalytics.models.ExtractKeyPhrasesAction;
import com.azure.ai.textanalytics.models.ExtractiveSummaryAction;
import com.azure.ai.textanalytics.models.ExtractiveSummarySentence;
import com.azure.ai.textanalytics.models.RecognizePiiEntitiesAction;
import com.azure.ai.textanalytics.models.SentimentConfidenceScores;
import com.azure.ai.textanalytics.models.TextAnalyticsActions;
import com.azure.ai.textanalytics.models.TextDocumentInput;
import com.azure.core.util.Context;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.BatchAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentDocument;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentSummaryResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.TextAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// Azure AI Language (formerly "Text Analytics") — the Cognitive Services NLP API.
// API docs: https://learn.microsoft.com/en-us/azure/ai-services/language-service/
//
// beginAnalyzeActions is a *long-running operation* (LRO): the SDK starts a server-side
// job, returns a poller, and getFinalResult() blocks until Azure finishes processing.
// one action set can bundle multiple analyses together so we pay one network round-trip.
//
// PII = Personally Identifiable Information. the RecognizePiiEntitiesAction scans text
// for things like social security numbers, email addresses, phone numbers, passport
// numbers, credit card numbers, physical addresses, etc. each detected entity has a
// category (e.g. "SSN", "Email", "PhoneNumber") and a confidence score 0.0-1.0.
// we only count the number of entities per comment — we don't redact or store them.
//
// AnalyzeSentimentAction returns one of "positive", "negative", "neutral", "mixed"
// per document plus confidence scores for each label (they sum to ~1.0).
//
// ExtractKeyPhrasesAction pulls out the most meaningful noun phrases from the text.
//
// ExtractiveSummaryAction picks the most representative sentences from the input
// (not generative AI — it just selects existing sentences, no hallucination risk).
//
// Azure limits: 25 documents per batch, 5120 characters per document.
// we chunk accordingly before sending.
public class TextAnalyticsServiceImpl implements TextAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(TextAnalyticsServiceImpl.class);
    private static final int BATCH_SIZE = 25;

    private final TextAnalyticsClient client;

    public TextAnalyticsServiceImpl(TextAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public BatchAnalysisResult analyzeBatch(List<CommentDocument> comments) {
        List<TextDocumentInput> allDocs = comments.stream()
                .map(c -> new TextDocumentInput(c.id(), c.text()))
                .toList();

        // per-document results keyed by document ID (the comment ID)
        Map<String, String> sentimentMap = new LinkedHashMap<>();       // "positive"/"negative"/"neutral"
        Map<String, Double> confidenceMap = new LinkedHashMap<>();      // highest of the 3 confidence scores
        Map<String, List<String>> keyPhrasesMap = new LinkedHashMap<>(); // noun phrases the API extracted
        Map<String, Integer> piiCountMap = new LinkedHashMap<>();       // count of PII entities found (SSN, email, phone, etc.)

        for (int i = 0; i < allDocs.size(); i += BATCH_SIZE) {
            List<TextDocumentInput> chunk = allDocs.subList(i, Math.min(i + BATCH_SIZE, allDocs.size()));
            processAnalysisChunk(chunk, sentimentMap, confidenceMap, keyPhrasesMap, piiCountMap);
        }

        List<CommentAnalysisResult> commentResults = comments.stream()
                .map(c -> new CommentAnalysisResult(
                        c.id(),
                        sentimentMap.getOrDefault(c.id(), "neutral"),
                        confidenceMap.getOrDefault(c.id(), 0.0),
                        keyPhrasesMap.getOrDefault(c.id(), List.of()),
                        piiCountMap.getOrDefault(c.id(), 0)
                )).toList();

        int positiveCount = (int) commentResults.stream()
                .filter(r -> "positive".equals(r.sentiment())).count();
        int negativeCount = (int) commentResults.stream()
                .filter(r -> "negative".equals(r.sentiment())).count();
        int neutralCount = commentResults.size() - positiveCount - negativeCount;

        String overallSentiment;
        if (positiveCount > negativeCount && positiveCount > neutralCount) {
            overallSentiment = "POSITIVE";
        } else if (negativeCount > positiveCount && negativeCount > neutralCount) {
            overallSentiment = "NEGATIVE";
        } else {
            overallSentiment = "MIXED";
        }

        double overallConfidence = commentResults.stream()
                .mapToDouble(CommentAnalysisResult::confidence)
                .average().orElse(0.0);

        List<String> topKeyPhrases = commentResults.stream()
                .flatMap(r -> r.keyPhrases().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .toList();

        int totalPii = commentResults.stream()
                .mapToInt(CommentAnalysisResult::piiEntityCount).sum();

        return new BatchAnalysisResult(
                overallSentiment, overallConfidence,
                positiveCount, neutralCount, negativeCount,
                topKeyPhrases, totalPii, commentResults);
    }

    private void processAnalysisChunk(
            List<TextDocumentInput> chunk,
            Map<String, String> sentimentMap,
            Map<String, Double> confidenceMap,
            Map<String, List<String>> keyPhrasesMap,
            Map<String, Integer> piiCountMap) {

        // bundle 3 analyses into one action set — Azure runs them in parallel server-side
        TextAnalyticsActions actions = new TextAnalyticsActions()
                .setAnalyzeSentimentActions(new AnalyzeSentimentAction())       // positive/negative/neutral
                .setExtractKeyPhrasesActions(new ExtractKeyPhrasesAction())     // meaningful noun phrases
                .setRecognizePiiEntitiesActions(new RecognizePiiEntitiesAction()); // SSN, email, phone, etc.

        // beginAnalyzeActions starts an LRO on Azure's side and returns a SyncPoller.
        // getFinalResult() blocks until the job completes (typically 2-10s depending on batch size).
        var poller = client.beginAnalyzeActions(chunk, actions, null, Context.NONE);
        var results = poller.getFinalResult();

        for (AnalyzeActionsResult page : results) {
            page.getAnalyzeSentimentResults().forEach(actionResult ->
                    actionResult.getDocumentsResults().forEach(r -> {
                        if (!r.isError()) {
                            DocumentSentiment ds = r.getDocumentSentiment();
                            sentimentMap.put(r.getId(), ds.getSentiment().toString().toLowerCase());
                            confidenceMap.put(r.getId(), maxConfidence(ds.getConfidenceScores()));
                        }
                    })
            );

            page.getExtractKeyPhrasesResults().forEach(actionResult ->
                    actionResult.getDocumentsResults().forEach(r -> {
                        if (!r.isError()) {
                            keyPhrasesMap.put(r.getId(),
                                    StreamSupport.stream(r.getKeyPhrases().spliterator(), false).toList());
                        }
                    })
            );

            // PII entities: each entity represents one detected piece of personally identifiable
            // information (e.g. "john@example.com" → Email, "123-45-6789" → US SSN).
            // we just count them — the actual text/category/offset is available but not stored.
            page.getRecognizePiiEntitiesResults().forEach(actionResult ->
                    actionResult.getDocumentsResults().forEach(r -> {
                        if (!r.isError()) {
                            piiCountMap.put(r.getId(),
                                    (int) StreamSupport.stream(r.getEntities().spliterator(), false).count());
                        }
                    })
            );
        }
    }

    @Override
    public CommentSummaryResult summarizeBatch(List<CommentDocument> comments) {
        StringBuilder sb = new StringBuilder();
        for (CommentDocument c : comments) {
            sb.append("[#").append(c.id()).append("] ").append(c.text()).append("\n");
        }
        String fullText = sb.toString();

        List<TextDocumentInput> docs = new ArrayList<>();
        for (int i = 0, docIdx = 0; i < fullText.length(); i += 5120, docIdx++) {
            String chunk = fullText.substring(i, Math.min(i + 5120, fullText.length()));
            docs.add(new TextDocumentInput(String.valueOf(docIdx), chunk));
        }

        // extractive summarization: picks up to 6 of the most representative sentences
        // from the input. this is NOT generative — it selects verbatim sentences, so there
        // is zero hallucination risk. Azure ranks sentences by centrality + redundancy.
        TextAnalyticsActions actions = new TextAnalyticsActions()
                .setExtractiveSummaryActions(new ExtractiveSummaryAction()
                        .setMaxSentenceCount(6));

        var poller = client.beginAnalyzeActions(docs, actions, null, Context.NONE);
        var results = poller.getFinalResult();

        List<String> sentences = new ArrayList<>();
        for (AnalyzeActionsResult page : results) {
            page.getExtractiveSummaryResults().forEach(actionResult ->
                    actionResult.getDocumentsResults().forEach(docResult -> {
                        if (!docResult.isError()) {
                            for (ExtractiveSummarySentence s : docResult.getSentences()) {
                                sentences.add(s.getText());
                            }
                        }
                    })
            );
        }

        String summaryText = sentences.isEmpty()
                ? "No summary could be generated."
                : String.join(" ", sentences);

        return new CommentSummaryResult(summaryText, comments.size());
    }

    private static double maxConfidence(SentimentConfidenceScores scores) {
        return Math.max(scores.getPositive(),
                Math.max(scores.getNeutral(), scores.getNegative()));
    }
}
