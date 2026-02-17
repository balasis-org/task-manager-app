/**
 * fileUtils.js
 *
 * Helpers for mapping file extensions to feather icons
 * and checking client-side file constraints.
 */

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

/* ───────── extension → icon mapping ───────── */

const EXT_MAP = {
    // Images
    jpg: FiImage, jpeg: FiImage, png: FiImage, gif: FiImage,
    bmp: FiImage, webp: FiImage, svg: FiImage, ico: FiImage,

    // Documents / text
    pdf: FiFileText, doc: FiFileText, docx: FiFileText,
    xls: FiFileText, xlsx: FiFileText, ppt: FiFileText,
    pptx: FiFileText, odt: FiFileText, ods: FiFileText,
    odp: FiFileText, txt: FiFileText, rtf: FiFileText,
    csv: FiFileText, md: FiFileText,

    // Audio
    mp3: FiMusic, wav: FiMusic, ogg: FiMusic, flac: FiMusic,
    aac: FiMusic, wma: FiMusic,

    // Video
    mp4: FiVideo, mov: FiVideo, avi: FiVideo, mkv: FiVideo,
    wmv: FiVideo, webm: FiVideo, flv: FiVideo,

    // Archives
    zip: FiArchive, rar: FiArchive, "7z": FiArchive,
    tar: FiArchive, gz: FiArchive, bz2: FiArchive,

    // Code
    js: FiCode, jsx: FiCode, ts: FiCode, tsx: FiCode,
    html: FiCode, css: FiCode, json: FiCode, xml: FiCode,
    java: FiCode, py: FiCode, c: FiCode, cpp: FiCode,
    cs: FiCode, go: FiCode, rs: FiCode, rb: FiCode,
    sh: FiCode, sql: FiCode, yml: FiCode, yaml: FiCode,
};

/**
 * Return the React Icon component for a filename.
 * @param {string} filename
 * @returns {import("react").ComponentType}
 */
export function getFileIcon(filename) {
    if (!filename) return FiFile;
    const ext = filename.split(".").pop()?.toLowerCase();
    return EXT_MAP[ext] || FiFile;
}

/* ───────── validation helpers ───────── */

/**
 * Check whether a file exceeds the max task-file size.
 * @param {File} file
 * @returns {boolean} true if the file is too big
 */
export function isFileTooLarge(file) {
    return file.size > LIMITS.MAX_FILE_SIZE_MB * 1024 * 1024;
}

/**
 * Check whether a file exceeds the max image size.
 * @param {File} file
 * @returns {boolean} true if the image is too big
 */
export function isImageTooLarge(file) {
    return file.size > LIMITS.MAX_IMAGE_SIZE_MB * 1024 * 1024;
}
