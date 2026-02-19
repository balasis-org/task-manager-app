import { useEffect, useRef, useState, useCallback } from "react";

/**
 * Generic smart-polling hook with tiered backoff.
 *
 * Tier 1: 30 s   for the first 10 min of inactivity (1 min after 30 min session)
 * Tier 2: 1 min  from 10 → 15 min of inactivity
 * Tier 3: STOP   after 15 min (isStale = true, refresh button only)
 *
 * @param {Function} checkFn   - async () => void.  Should throw (or return truthy)
 *                                when "something changed" (e.g. 409 from server).
 * @param {Object}   opts
 * @param {number}   opts.tier1Ms      - active polling interval      (default 30 000)
 * @param {number}   opts.tier1LongMs  - active interval (long sess.) (default 60 000)
 * @param {number}   opts.tier1Until   - tier-1 window                (default 10 min)
 * @param {number}   opts.tier2Ms      - mild-idle polling            (default 60 000)
 * @param {number}   opts.tier2Until   - tier-2 window                (default 15 min)
 * @param {number}   opts.longSessionMs - long session threshold      (default 30 min)
 * @param {boolean}  opts.enabled      - master on/off (default true)
 *
 * @returns {{ hasChanged: boolean, isStale: boolean, reset: () => void }}
 */
export default function useSmartPoll(checkFn, {
    tier1Ms      = 30_000,
    tier1LongMs  = 60_000,
    tier1Until   = 10 * 60_000,
    tier2Ms      = 60_000,
    tier2Until   = 15 * 60_000,
    longSessionMs = 30 * 60_000,
    enabled      = true,
} = {}) {
    const [hasChanged, setHasChanged] = useState(false);
    const [isStale, setIsStale]       = useState(false);

    const timer        = useRef(null);
    const lastActivity = useRef(Date.now());
    const mountTime    = useRef(Date.now());
    const checkRef     = useRef(checkFn);
    checkRef.current = checkFn;

    function getInterval() {
        const idle = Date.now() - lastActivity.current;
        if (idle >= tier2Until) return null;          // stop
        if (idle >= tier1Until) return tier2Ms;       // 1 min

        const sessionAge = Date.now() - mountTime.current;
        return sessionAge > longSessionMs ? tier1LongMs : tier1Ms;
    }

    const schedule = useCallback(() => {
        if (timer.current) clearTimeout(timer.current);

        const interval = getInterval();
        if (interval === null) {
            setIsStale(true);
            return;
        }

        timer.current = setTimeout(async () => {
            const idle = Date.now() - lastActivity.current;
            if (idle >= tier2Until) {
                setIsStale(true);
                return;
            }

            try {
                await checkRef.current();
            } catch (err) {
                if (err?.status === 409) {
                    setHasChanged(true);
                }
            }

            schedule();   // re-schedule at the (possibly new) tier
        }, interval);
    }, [tier1Ms, tier1LongMs, tier1Until, tier2Ms, tier2Until, longSessionMs]);

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
