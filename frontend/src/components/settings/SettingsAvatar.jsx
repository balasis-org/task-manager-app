import { useState, useRef, useContext } from "react";
import { FiCamera, FiUser } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { useToast } from "@context/ToastContext";
import { apiPatch, apiMultipart } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import { isImageTooLarge } from "@assets/js/fileUtils";
import { useBlobUrl } from "@context/BlobSasContext";
import DefaultImagePicker from "@components/DefaultImagePicker";
import "@styles/settings/SettingsAvatar.css";

// profile image upload — drag-drop or click, with DefaultImagePicker fallback.
// GIFs are blocked client-side (the backend also rejects them).
// bumps usedImageScansMonth locally to keep the Settings budget bar accurate
// before the next /users/me refresh.
export default function SettingsAvatar() {
    const { user, setUser } = useContext(AuthContext);
    const showToast = useToast();
    const blobUrl = useBlobUrl();

    const [uploadingImg, setUploadingImg] = useState(false);
    const [imgDragOver, setImgDragOver] = useState(false);
    const [showDefaultPicker, setShowDefaultPicker] = useState(false);
    const fileRef = useRef(null);

    function handleImagePick(file) {
        if (!file) return;
        if (!file.type.startsWith("image/")) {
            showToast("Only image files are allowed");
            return;
        }
        if (file.type === "image/gif") {
            showToast("GIF images are not supported. Please use PNG or JPG.");
            return;
        }
        if (isImageTooLarge(file)) {
            showToast(`Image must be under ${LIMITS.MAX_IMAGE_SIZE_MB} MB`);
            return;
        }
        uploadImage(file);
    }

    async function uploadImage(file) {
        setUploadingImg(true);
        try {
            const fd = new FormData();
            fd.append("file", file);
            const updated = await apiMultipart("/api/users/me/profile-image", fd);
            setUser((prev) => (prev ? {
                ...prev,
                imgUrl: updated.imgUrl,
                usedImageScansMonth: (prev.usedImageScansMonth ?? 0) + 1,
            } : prev));
            showToast("Profile image updated!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to upload image");
        } finally {
            setUploadingImg(false);
        }
    }

    async function pickDefaultImage(fileName) {
        setUploadingImg(true);
        try {
            const updated = await apiPatch(`/api/users/me/profile-image/pick-default?fileName=${encodeURIComponent(fileName)}`);
            setUser((prev) => (prev ? { ...prev, imgUrl: updated.imgUrl, defaultImgUrl: updated.defaultImgUrl } : prev));
            showToast("Profile image updated!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to apply default image");
        } finally {
            setUploadingImg(false);
        }
    }

    async function pickMicrosoftImage() {
        setUploadingImg(true);
        try {
            const updated = await apiPatch("/api/users/me/profile-image/pick-microsoft");
            setUser((prev) => (prev ? { ...prev, imgUrl: updated.imgUrl, msProfilePhotoUrl: updated.msProfilePhotoUrl } : prev));
            showToast("Now using your Microsoft photo!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to apply Microsoft photo");
        } finally {
            setUploadingImg(false);
        }
    }

    const isFree = user?.subscriptionPlan === "FREE";
    const atScanLimit = user?.imageScansPerMonth > 0
        && (user?.usedImageScansMonth ?? 0) >= user.imageScansPerMonth;

    const imgSrc = user?.imgUrl
        ? blobUrl(user.imgUrl)
        : user?.msProfilePhotoUrl
            ? blobUrl(user.msProfilePhotoUrl)
            : user?.defaultImgUrl
                ? blobUrl(user.defaultImgUrl)
                : null;

    return (
        <section className="settings-card settings-card-avatar">
            {isFree ? (
                /* FREE tier: show avatar but no upload zone */
                <div className="settings-avatar-zone settings-avatar-zone-static">
                    {imgSrc ? (
                        <img src={imgSrc} alt="Profile" className="settings-avatar-img" />
                    ) : (
                        <div className="settings-avatar-placeholder">
                            <FiUser size={40} />
                        </div>
                    )}
                </div>
            ) : atScanLimit ? (
                /* Paid tier but at monthly image limit */
                <div className="settings-avatar-zone settings-avatar-zone-static settings-avatar-zone-disabled">
                    {imgSrc ? (
                        <img src={imgSrc} alt="Profile" className="settings-avatar-img" />
                    ) : (
                        <div className="settings-avatar-placeholder">
                            <FiUser size={40} />
                        </div>
                    )}
                </div>
            ) : (
                <div
                    className={`settings-avatar-zone${imgDragOver ? " drag-over" : ""}`}
                    onDragOver={(e) => { e.preventDefault(); setImgDragOver(true); }}
                    onDragLeave={() => setImgDragOver(false)}
                    onDrop={(e) => {
                        e.preventDefault();
                        setImgDragOver(false);
                        handleImagePick(e.dataTransfer.files?.[0]);
                    }}
                    onClick={() => fileRef.current?.click()}
                    title="Click or drop an image to change your photo"
                >
                    {imgSrc ? (
                        <img src={imgSrc} alt="Profile" className="settings-avatar-img" />
                    ) : (
                        <div className="settings-avatar-placeholder">
                            <FiUser size={40} />
                        </div>
                    )}
                    <div className="settings-avatar-overlay">
                        {uploadingImg ? (
                            <span className="settings-avatar-spinner" />
                        ) : (
                            <FiCamera size={22} />
                        )}
                    </div>
                    <input
                        ref={fileRef}
                        type="file"
                        accept="image/png, image/jpeg, image/webp"
                        hidden
                        onChange={(e) => {
                            handleImagePick(e.target.files?.[0]);
                            // reset so picking the same file again still fires onChange
                            e.target.value = "";
                        }}
                    />
                </div>
            )}
            {isFree ? (
                <span className="settings-avatar-hint settings-avatar-hint-upgrade">Upgrade to upload custom images</span>
            ) : atScanLimit ? (
                <span className="settings-avatar-hint settings-avatar-hint-limit">Reached the limit</span>
            ) : (
                <span className="settings-avatar-hint">Click or drag to change photo</span>
            )}
            <button
                type="button"
                className="settings-btn settings-btn-secondary settings-defaults-btn"
                onClick={() => setShowDefaultPicker(true)}
                disabled={uploadingImg}
            >
                Pick from defaults
            </button>
            {user?.msProfilePhotoUrl && (
                <button
                    type="button"
                    className="settings-btn settings-btn-secondary settings-defaults-btn"
                    onClick={pickMicrosoftImage}
                    disabled={uploadingImg}
                >
                    Use Microsoft photo
                </button>
            )}
            {showDefaultPicker && (
                <DefaultImagePicker
                    type="PROFILE_IMAGES"
                    onPick={pickDefaultImage}
                    onClose={() => setShowDefaultPicker(false)}
                />
            )}
        </section>
    );
}
