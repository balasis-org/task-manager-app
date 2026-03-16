// Group cover image - drag-drop upload or pick from defaults
import { useState, useRef, useContext, useMemo } from "react";
import { FiCheck, FiX, FiImage } from "react-icons/fi";
import { apiPatch, apiMultipart } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { LIMITS } from "@assets/js/inputValidation";
import { isImageTooLarge } from "@assets/js/fileUtils";
import { useBlobUrl } from "@context/BlobSasContext";
import { AuthContext } from "@context/AuthContext";
import DefaultImagePicker from "@components/DefaultImagePicker";
import "@styles/groupsettings/GsImageSection.css";

export default function GsImageSection({ group, ownerPlan, onUpdated }) {
    const showToast = useToast();
    const blobUrl = useBlobUrl();
    const { user, updateUser } = useContext(AuthContext);

    const [coverImage, setCoverImage] = useState(null);
    const [uploadingImg, setUploadingImg] = useState(false);
    const [imgDragOver, setImgDragOver] = useState(false);
    const [showDefaultPicker, setShowDefaultPicker] = useState(false);
    const [imgVersion, setImgVersion] = useState(0);
    const fileRef = useRef(null);

    // local preview URL for the file the user picked (before uploading)
    const localPreview = useMemo(() => {
        if (!coverImage) return null;
        return URL.createObjectURL(coverImage);
    }, [coverImage]);

    function cancelPick() {
        setCoverImage(null);
        if (fileRef.current) fileRef.current.value = "";
    }

    async function uploadImage() {
        if (!coverImage) return;
        setUploadingImg(true);
        try {
            const fd = new FormData();
            fd.append("file", coverImage);
            const updated = await apiMultipart(`/api/groups/${group.id}/image`, fd);
            onUpdated(updated);
            setImgVersion(v => v + 1);
            updateUser({ usedImageScansMonth: (user?.usedImageScansMonth ?? 0) + 1 });
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
            setImgVersion(v => v + 1);
            showToast("Group image updated", "success");
        } catch (err) {
            showToast(err?.message || "Failed to apply default image");
        } finally {
            setUploadingImg(false);
        }
    }

    function handleFileDrop(e) {
        e.preventDefault();
        setImgDragOver(false);
        if (atScanLimit) return;
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

    const isFreeOwner = ownerPlan === "FREE";
    const isOwner = user?.id === group.ownerId;
    const atScanLimit = isOwner
        && user?.imageScansPerMonth > 0
        && (user?.usedImageScansMonth ?? 0) >= user.imageScansPerMonth;

    function imgSrc() {
        const base = group.imgUrl ? blobUrl(group.imgUrl) : blobUrl(group.defaultImgUrl);
        return imgVersion ? `${base}&_v=${imgVersion}` : base;
    }

    // What to show on the left thumbnail: local preview if file picked, else current image
    const thumbSrc = localPreview || ((group.imgUrl || group.defaultImgUrl) ? imgSrc() : null);

    return (
        <section className="gs-section">
            <div className="gs-section-header">
                <span className="gs-section-label"><FiImage size={14} /> Group image</span>
                {atScanLimit && (
                    <span className="gs-limit-note">Reached the limit</span>
                )}
            </div>
            {!isFreeOwner && (
                <div
                    className={`gs-image-row${imgDragOver ? " drag-over" : ""}${atScanLimit && !coverImage ? " gs-image-row-disabled" : ""}`}
                    onDragOver={(e) => { e.preventDefault(); if (!atScanLimit) setImgDragOver(true); }}
                    onDragLeave={() => setImgDragOver(false)}
                    onDrop={handleFileDrop}
                >
                    {thumbSrc && (
                        <img
                            src={thumbSrc}
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
                    {coverImage ? (
                        /* File picked → show filename, cancel, upload */
                        <div className="gs-pick-actions">
                            <span className="gs-pick-filename" title={coverImage.name}>{coverImage.name}</span>
                            <button className="gs-cancel-btn" onClick={cancelPick} disabled={uploadingImg}>
                                <FiX size={13} /> Cancel
                            </button>
                            <button className="gs-save-btn" onClick={uploadImage} disabled={uploadingImg}>
                                <FiCheck size={13} /> {uploadingImg ? "Uploading…" : "Upload"}
                            </button>
                        </div>
                    ) : (
                        /* No file picked → choose file button */
                        <button
                            className="gs-upload-btn"
                            onClick={() => fileRef.current?.click()}
                            disabled={uploadingImg || atScanLimit}
                        >
                            Choose file
                        </button>
                    )}
                </div>
            )}
            {isFreeOwner && (
                <div className="gs-image-row">
                    {thumbSrc && (
                        <img
                            src={thumbSrc}
                            alt="Group"
                            className="gs-group-thumb"
                        />
                    )}
                    <span className="gs-free-hint">Upgrade to upload custom images</span>
                </div>
            )}
            {/* Hide "Pick from defaults" when a file is already staged for upload */}
            {!coverImage && (
                <button
                    className="gs-defaults-btn"
                    onClick={() => setShowDefaultPicker(true)}
                    disabled={uploadingImg}
                >
                    Pick from defaults
                </button>
            )}
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
