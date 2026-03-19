import { useState, useEffect, useRef } from "react";
import { apiGet } from "@assets/js/apiClient.js";

const POLL_INTERVAL = 5 * 60_000; // 5 minutes

export default function useAiStatus() {
    const [aiStatus, setAiStatus] = useState(null);
    const timer = useRef(null);

    useEffect(() => {
        let cancelled = false;

        async function fetchStatus() {
            try {
                const data = await apiGet("/api/ai-status");
                if (!cancelled) setAiStatus(data);
            } catch {
                // endpoint unreachable — clear status so banner hides
                if (!cancelled) setAiStatus(null);
            }
            if (!cancelled) {
                timer.current = setTimeout(fetchStatus, POLL_INTERVAL);
            }
        }

        fetchStatus();

        return () => {
            cancelled = true;
            if (timer.current) clearTimeout(timer.current);
        };
    }, []);

    return aiStatus;
}
