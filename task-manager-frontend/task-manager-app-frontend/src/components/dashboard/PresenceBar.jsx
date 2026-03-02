import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/dashboard/PresenceBar.css";

/**
 * Stacked avatar row showing which group members are currently viewing the dashboard.
 * Matches presence user-IDs against the already-loaded members array — zero extra backend calls.
 */
export default function PresenceBar({ members, presenceUserIds, currentUserId }) {
    const blobUrl = useBlobUrl();

    if (!presenceUserIds?.length || !members?.length) return null;

    // Map IDs → member objects, skip any that aren't in the local members list
    const onlineMembers = presenceUserIds
        .map(id => members.find(m => m.user?.id === id))
        .filter(Boolean);

    if (onlineMembers.length === 0) return null;

    // Current user first, then alphabetical by name
    const sorted = [...onlineMembers].sort((a, b) => {
        if (a.user?.id === currentUserId) return -1;
        if (b.user?.id === currentUserId) return 1;
        return (a.user?.name || "").localeCompare(b.user?.name || "");
    });

    const MAX_AVATARS = 5;
    const shown = sorted.slice(0, MAX_AVATARS);
    const overflow = sorted.length - MAX_AVATARS;

    return (
        <div
            className="presence-bar"
            title={`${sorted.length} member${sorted.length !== 1 ? "s" : ""} online`}
        >
            <div className="presence-avatars">
                {shown.map((m, i) => (
                    <div
                        key={m.user?.id}
                        className={`presence-avatar${m.user?.id === currentUserId ? " presence-avatar-me" : ""}`}
                        title={m.user?.id === currentUserId ? "You" : (m.user?.name || m.user?.email)}
                        style={{ zIndex: shown.length - i }}
                    >
                        <img
                            src={
                                m.user?.imgUrl
                                    ? blobUrl(m.user.imgUrl)
                                    : m.user?.defaultImgUrl
                                        ? blobUrl(m.user.defaultImgUrl)
                                        : ""
                            }
                            alt=""
                            className="presence-avatar-img"
                        />
                        <span className="presence-dot" />
                    </div>
                ))}
                {overflow > 0 && (
                    <div className="presence-overflow" style={{ zIndex: 0 }}>
                        +{overflow}
                    </div>
                )}
            </div>
            <span className="presence-label">
                {sorted.length} online
            </span>
        </div>
    );
}
