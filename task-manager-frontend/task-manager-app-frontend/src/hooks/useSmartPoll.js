import { useEffect, useRef, useState, useCallback } from "react";

/**
 * Generic smart-polling hook with tiered backoff.
 *
 * Tier 1: 20 s   for the first 10 min of inactivity
 * Tier 2: 1 min  from 10 → 15 min of inactivity
 * Tier 3: 15 min after 15 min (+ isStale flag for UI)
 *
 * @param {Function} checkFn   - async () => void.  Should throw (or return truthy)
 *                                when "something changed" (e.g. 409 from server).
 * @param {Object}   opts
 * @param {number}   opts.tier1Ms    - active polling interval   (default 20 000)
 * @param {number}   opts.tier1Until - tier-1 window             (default 10 min)
 * @param {number}   opts.tier2Ms    - mild-idle polling         (default 60 000)
 * @param {number}   opts.tier2Until - tier-2 window             (default 15 min)
 * @param {number}   opts.tier3Ms    - deep-idle polling         (default 15 min)
 * @param {boolean}  opts.enabled    - master on/off (default true)
 *
 * @returns {{ hasChanged: boolean, isStale: boolean, reset: () => void }}
 */
export default function useSmartPoll(checkFn, {
    tier1Ms    = 20_000,
    tier1Until = 10 * 60_000,
    tier2Ms    = 60_000,
    tier2Until = 15 * 60_000,
    tier3Ms    = 15 * 60_000,
    enabled    = true,
} = {}) {
    const [hasChanged, setHasChanged] = useState(false);
    const [isStale, setIsStale]       = useState(false);

    const timer        = useRef(null);
    const lastActivity = useRef(Date.now());
    const checkRef     = useRef(checkFn);
    checkRef.current = checkFn;

    function getInterval() {
        const idle = Date.now() - lastActivity.current;
        if (idle < tier1Until) return tier1Ms;
        if (idle < tier2Until) return tier2Ms;
        return tier3Ms;
    }

    const schedule = useCallback(() => {
        if (timer.current) clearTimeout(timer.current);

        timer.current = setTimeout(async () => {
            const idle = Date.now() - lastActivity.current;

            // show refresh banner once we enter tier 3
            if (idle >= tier2Until) setIsStale(true);

            try {
                await checkRef.current();
            } catch (err) {
                if (err?.status === 409) {
                    setHasChanged(true);
                }
            }

            schedule();   // re-schedule at the (possibly new) tier
        }, getInterval());
    }, [tier1Ms, tier1Until, tier2Ms, tier2Until, tier3Ms]);

    // reset everything — call after user re-fetches data
    const reset = useCallback(() => {
        lastActivity.current = Date.now();
        setHasChanged(false);
        setIsStale(false);
        schedule();
    }, [schedule]);

    // user-activity listener resets the idle clock
    useEffect(() => {
        if (!enabled) return;
        const onActivity = () => {
            lastActivity.current = Date.now();
            if (isStale) setIsStale(false);
            schedule();
        };
        const events = ["click", "keydown", "scroll", "pointerdown"];
        events.forEach(ev => window.addEventListener(ev, onActivity, { passive: true }));
        return () => events.forEach(ev => window.removeEventListener(ev, onActivity));
    }, [enabled, isStale, schedule]);

    // start / stop
    useEffect(() => {
        if (!enabled) {
            if (timer.current) clearTimeout(timer.current);
            return;
        }
        lastActivity.current = Date.now();
        setHasChanged(false);
        setIsStale(false);
        schedule();
        return () => { if (timer.current) clearTimeout(timer.current); };
    }, [enabled, schedule]);

    return { hasChanged, isStale, reset };
}
