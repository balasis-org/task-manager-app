import { useEffect, useRef, useState, useCallback } from "react";

/**
 * Generic smart-polling hook with progressive backoff.
 *
 * @param {Function} checkFn   - async () => void.  Should throw (or return truthy)
 *                                when "something changed" (e.g. 409 from server).
 * @param {Object}   opts
 * @param {number}   opts.baseMs      - starting interval  (default 30 000)
 * @param {number}   opts.maxMs       - ceiling interval    (default 5 min)
 * @param {number}   opts.staleMs     - stop polling after this idle time (default 5 min)
 * @param {boolean}  opts.enabled     - master on/off (default true)
 *
 * @returns {{ hasChanged: boolean, isStale: boolean, reset: () => void }}
 */
export default function useSmartPoll(checkFn, {
    baseMs  = 30_000,
    maxMs   = 5 * 60_000,
    staleMs = 5 * 60_000,
    enabled = true,
} = {}) {
    const [hasChanged, setHasChanged] = useState(false);
    const [isStale, setIsStale]       = useState(false);

    const timer        = useRef(null);
    const interval     = useRef(baseMs);
    const lastActivity = useRef(Date.now());
    const checkRef     = useRef(checkFn);
    checkRef.current = checkFn;

    const schedule = useCallback(() => {
        if (timer.current) clearTimeout(timer.current);

        timer.current = setTimeout(async () => {
            const idle = Date.now() - lastActivity.current;

            if (idle >= staleMs) {
                setIsStale(true);
                return; // stop polling
            }

            try {
                await checkRef.current();
                // no change
            } catch (err) {
                if (err?.status === 409) {
                    setHasChanged(true);
                }
                // other errors silently ignored
            }

            interval.current = Math.min(interval.current * 2, maxMs);
            schedule();
        }, interval.current);
    }, [maxMs, staleMs]);

    // reset everything — call after user re-fetches data
    const reset = useCallback(() => {
        lastActivity.current = Date.now();
        interval.current = baseMs;
        setHasChanged(false);
        setIsStale(false);
        schedule();
    }, [baseMs, schedule]);

    // user-activity listener resets the backoff
    useEffect(() => {
        if (!enabled) return;
        const onActivity = () => {
            lastActivity.current = Date.now();
            if (isStale) {
                setIsStale(false);
            }
            interval.current = baseMs;
            schedule();
        };
        const events = ["click", "keydown", "scroll", "pointerdown"];
        events.forEach(ev => window.addEventListener(ev, onActivity, { passive: true }));
        return () => events.forEach(ev => window.removeEventListener(ev, onActivity));
    }, [enabled, baseMs, isStale, schedule]);

    // start / stop
    useEffect(() => {
        if (!enabled) {
            if (timer.current) clearTimeout(timer.current);
            return;
        }
        interval.current = baseMs;
        lastActivity.current = Date.now();
        setHasChanged(false);
        setIsStale(false);
        schedule();
        return () => { if (timer.current) clearTimeout(timer.current); };
    }, [enabled, baseMs, schedule]);

    return { hasChanged, isStale, reset };
}
