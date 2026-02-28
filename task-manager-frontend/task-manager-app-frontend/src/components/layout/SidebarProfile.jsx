import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/layout/SidebarProfile.css";

export default function SidebarProfile({ user }) {
    const blobUrl = useBlobUrl();

    const rawImgPath = user?.imgUrl || user?.defaultImgUrl || null;
    const profileImg = rawImgPath ? blobUrl(rawImgPath) : null;

    return (
        <div className="sidebar-profile">
            {profileImg ? (
                <img
                    src={profileImg}
                    alt="profile"
                    className="sidebar-profile-img"
                />
            ) : (
                <div className="sidebar-profile-img" />
            )}
            {user?.name && (
                <span className="sidebar-profile-name">{user.name}</span>
            )}
            {user?.inviteCode && (
                <span
                    className="sidebar-profile-code"
                    title="Your invite code — share it so others can invite you"
                    onClick={() => {
                        navigator.clipboard.writeText(user.inviteCode);
                    }}
                >
                    {user.inviteCode}
                </span>
            )}
        </div>
    );
}
