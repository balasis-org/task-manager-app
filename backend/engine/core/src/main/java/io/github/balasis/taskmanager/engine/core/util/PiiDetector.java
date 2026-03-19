package io.github.balasis.taskmanager.engine.core.util;

import java.util.regex.Pattern;

// Local PII detector — regex-based, no external calls, runs synchronously.
// Covers: email addresses, phone numbers, credit card numbers, IBANs.
// Applied on every comment create/edit for all tiers (GDPR requirement, not tier-gated).
public final class PiiDetector {

    private PiiDetector() {}

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    private static final Pattern PHONE =
            Pattern.compile("(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{1,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}");

    private static final Pattern CREDIT_CARD =
            Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

    private static final Pattern IBAN =
            Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{4,30}\\b");

    public static boolean containsPii(String text) {
        if (text == null || text.isBlank()) return false;
        return EMAIL.matcher(text).find()
                || PHONE.matcher(text).find()
                || CREDIT_CARD.matcher(text).find()
                || IBAN.matcher(text).find();
    }
}
