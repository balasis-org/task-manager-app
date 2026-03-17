package io.github.balasis.taskmanager.context.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

// Strips the body of octet-stream responses (file downloads) to eliminate
// egress costs during arena stress testing.
//
// JSON and other content types pass through untouched so k6 can still
// read response bodies (IDs, status, etc.).
//
// Spring sets Content-Type from the ResponseEntity builder BEFORE dispatching
// StreamingResponseBody to the async thread, so the content type is known
// when getOutputStream() is called.
@Component
@Profile("prod-arena-stress")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StressResponseFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, new StrippingResponseWrapper(response));
    }

    private static class StrippingResponseWrapper extends HttpServletResponseWrapper {

        private String storedContentType;
        private long originalContentLength = -1;
        private ServletOutputStream wrappedStream;

        StrippingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setContentType(String type) {
            this.storedContentType = type;
            super.setContentType(type);
        }

        @Override
        public void setContentLengthLong(long len) {
            if (isOctetStream()) {
                this.originalContentLength = len;
                super.addHeader("X-Arena-Original-Size", String.valueOf(len));
                super.setContentLengthLong(0);
            } else {
                super.setContentLengthLong(len);
            }
        }

        @Override
        public void setContentLength(int len) {
            setContentLengthLong(len);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (wrappedStream != null) {
                return wrappedStream;
            }
            if (isOctetStream()) {
                ServletOutputStream real = super.getOutputStream();
                wrappedStream = new VoidOutputStream(real);
                return wrappedStream;
            }
            return super.getOutputStream();
        }

        private boolean isOctetStream() {
            return storedContentType != null
                    && storedContentType.contains("octet-stream");
        }
    }

    // Counts bytes written but discards them. Delegates lifecycle methods
    // (isReady, setWriteListener) to the real stream so async dispatch works.
    private static class VoidOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;
        private final AtomicLong byteCount = new AtomicLong();

        VoidOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) {
            byteCount.incrementAndGet();
        }

        @Override
        public void write(byte[] b, int off, int len) {
            byteCount.addAndGet(len);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }

        @Override
        public void flush() {
            // no-op: nothing to flush
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
