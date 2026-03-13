
import {
    FiFile,
    FiFileText,
    FiImage,
    FiMusic,
    FiVideo,
    FiArchive,
    FiCode,
} from "react-icons/fi";
import { LIMITS } from "@assets/js/inputValidation";

const EXT_MAP = {

    jpg: FiImage, jpeg: FiImage, png: FiImage, gif: FiImage,
    bmp: FiImage, webp: FiImage, svg: FiImage, ico: FiImage,

    pdf: FiFileText, doc: FiFileText, docx: FiFileText,
    xls: FiFileText, xlsx: FiFileText, ppt: FiFileText,
    pptx: FiFileText, odt: FiFileText, ods: FiFileText,
    odp: FiFileText, txt: FiFileText, rtf: FiFileText,
    csv: FiFileText, md: FiFileText,

    mp3: FiMusic, wav: FiMusic, ogg: FiMusic, flac: FiMusic,
    aac: FiMusic, wma: FiMusic,

    mp4: FiVideo, mov: FiVideo, avi: FiVideo, mkv: FiVideo,
    wmv: FiVideo, webm: FiVideo, flv: FiVideo,

    zip: FiArchive, rar: FiArchive, "7z": FiArchive,
    tar: FiArchive, gz: FiArchive, bz2: FiArchive,

    js: FiCode, jsx: FiCode, ts: FiCode, tsx: FiCode,
    html: FiCode, css: FiCode, json: FiCode, xml: FiCode,
    java: FiCode, py: FiCode, c: FiCode, cpp: FiCode,
    cs: FiCode, go: FiCode, rs: FiCode, rb: FiCode,
    sh: FiCode, sql: FiCode, yml: FiCode, yaml: FiCode,
};

export function getFileIcon(filename) {
    if (!filename) return FiFile;
    const ext = filename.split(".").pop()?.toLowerCase();
    return EXT_MAP[ext] || FiFile;
}

/**
 * Returns true when the file exceeds the given byte limit.
 * Falls back to 5 MB if no limit is supplied (safest FREE default).
 */
export function isFileTooLarge(file, maxBytes) {
    const limit = maxBytes ?? (5 * 1024 * 1024);
    return file.size > limit;
}

export function isImageTooLarge(file) {
    return file.size > LIMITS.MAX_IMAGE_SIZE_MB * 1024 * 1024;
}

/** Human-readable file size: "1.2 MB", "340 KB", etc. */
export function formatFileSize(bytes) {
    if (bytes == null) return "";
    if (bytes < 1024)              return bytes + " B";
    if (bytes < 1024 * 1024)       return (bytes / 1024).toFixed(1) + " KB";
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
}
