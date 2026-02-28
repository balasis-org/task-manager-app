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
            setUser((prev) => (prev ? { ...prev, imgUrl: updated.imgUrl } : prev));
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

    const imgSrc = user?.imgUrl
        ? blobUrl(user.imgUrl)
        : user?.defaultImgUrl
            ? blobUrl(user.defaultImgUrl)
            : null;

    return (
        <section className="settings-card settings-card-avatar">
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
            <span className="settings-avatar-hint">Click or drag to change photo</span>
            <button
                type="button"
                className="settings-btn settings-btn-secondary settings-defaults-btn"
                onClick={() => setShowDefaultPicker(true)}
                disabled={uploadingImg}
            >
                Pick from defaults
            </button>
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
