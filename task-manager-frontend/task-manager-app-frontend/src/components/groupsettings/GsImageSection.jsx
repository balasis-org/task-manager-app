// Group cover image — drag-drop upload or pick from defaults
import { useState, useRef } from "react";
import { FiCheck } from "react-icons/fi";
import { apiPatch, apiMultipart } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { LIMITS } from "@assets/js/inputValidation";
import { isImageTooLarge } from "@assets/js/fileUtils";
import { useBlobUrl } from "@context/BlobSasContext";
import DefaultImagePicker from "@components/DefaultImagePicker";
import "@styles/groupsettings/GsImageSection.css";

export default function GsImageSection({ group, onUpdated }) {
    const showToast = useToast();
    const blobUrl = useBlobUrl();

    const [coverImage, setCoverImage] = useState(null);
    const [uploadingImg, setUploadingImg] = useState(false);
    const [imgDragOver, setImgDragOver] = useState(false);
    const [showDefaultPicker, setShowDefaultPicker] = useState(false);
    const fileRef = useRef(null);

    async function uploadImage() {
        if (!coverImage) return;
        setUploadingImg(true);
        try {
            const fd = new FormData();
            fd.append("file", coverImage);
            const updated = await apiMultipart(`/api/groups/${group.id}/image`, fd);
            onUpdated(updated);
            showToast("Group image updated", "success");
            setCoverImage(null);
        } catch (err) {
            showToast(err?.message || "Failed to upload image");
        } finally {
            setUploadingImg(false);
        }
    }

    async function pickDefaultImage(fileName) {
        setUploadingImg(true);
        try {
            const updated = await apiPatch(
                `/api/groups/${group.id}/image/pick-default?fileName=${encodeURIComponent(fileName)}`
            );
            onUpdated(updated);
            showToast("Group image updated", "success");
        } catch (err) {
            showToast(err?.message || "Failed to apply default image");
        } finally {
            setUploadingImg(false);
        }
    }

    // GIF check is duplicated between drop and input handlers on purpose —
    // drop gives us the file directly, input goes through e.target.files,
    // and the early-return + toast paths are slightly different.
    function handleFileDrop(e) {
        e.preventDefault();
        setImgDragOver(false);
        const file = e.dataTransfer.files?.[0];
        if (!file) return;
        if (!file.type.startsWith("image/")) { showToast("Only image files are allowed"); return; }
        if (file.type === "image/gif") { showToast("GIF images are not supported. Please use PNG or JPG."); return; }
        if (isImageTooLarge(file)) { showToast(`Image must be under ${LIMITS.MAX_IMAGE_SIZE_MB} MB`); return; }
        setCoverImage(file);
    }

    function handleFileInput(e) {
        const f = e.target.files?.[0] || null;
        if (f && f.type === "image/gif") { showToast("GIF images are not supported. Please use PNG or JPG."); e.target.value = ""; return; }
        if (f && isImageTooLarge(f)) { showToast(`Image must be under ${LIMITS.MAX_IMAGE_SIZE_MB} MB`); e.target.value = ""; return; }
        setCoverImage(f);
        e.target.value = "";
    }

    return (
        <section className="gs-section">
            <div className="gs-section-header">
                <span className="gs-section-label">Group image</span>
            </div>
            <div
                className={`gs-image-row${imgDragOver ? " drag-over" : ""}`}
                onDragOver={(e) => { e.preventDefault(); setImgDragOver(true); }}
                onDragLeave={() => setImgDragOver(false)}
                onDrop={handleFileDrop}
            >
                {(group.imgUrl || group.defaultImgUrl) && (
                    <img
                        src={group.imgUrl ? blobUrl(group.imgUrl) : blobUrl(group.defaultImgUrl)}
                        alt="Group"
                        className="gs-group-thumb"
                    />
                )}
                <input
                    ref={fileRef}
                    type="file"
                    accept="image/png, image/jpeg, image/webp"
                    hidden
                    onChange={handleFileInput}
                />
                <button className="gs-upload-btn" onClick={() => fileRef.current?.click()} disabled={uploadingImg}>
                    {coverImage ? coverImage.name : "Choose file"}
                </button>
                {coverImage && (
                    <button className="gs-save-btn" onClick={uploadImage} disabled={uploadingImg}>
                        <FiCheck size={13} /> {uploadingImg ? "Uploading…" : "Upload"}
                    </button>
                )}
            </div>
            <button
                className="gs-defaults-btn"
                onClick={() => setShowDefaultPicker(true)}
                disabled={uploadingImg}
            >
                Pick from defaults
            </button>
            {showDefaultPicker && (
                <DefaultImagePicker
                    type="GROUP_IMAGES"
                    onPick={pickDefaultImage}
                    onClose={() => setShowDefaultPicker(false)}
                />
            )}
        </section>
    );
}
