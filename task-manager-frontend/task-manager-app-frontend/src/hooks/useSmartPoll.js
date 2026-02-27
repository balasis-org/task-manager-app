import { useEffect, useRef, useState, useCallback } from "react";

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
        if (idle >= tier2Until) return null;
        if (idle >= tier1Until) return tier2Ms;

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

            schedule();
        }, interval);
    }, [tier1Ms, tier1LongMs, tier1Until, tier2Ms, tier2Until, longSessionMs]);

    const reset = useCallback(() => {
        lastActivity.current = Date.now();
        setHasChanged(false);
        setIsStale(false);
        schedule();
    }, [schedule]);

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
