const validateRequired = (value, fieldName = "This field") => {
    if (!value.trim()) return `${fieldName} is required`;
    return null;
};

const validatePattern = (value, regex, message) => {
    if (!regex.test(value)) return message;
    return null;
};

const InputCheck = {
    email: (value) => {
        const requiredErr = validateRequired(value, "Email");
        if (requiredErr) return requiredErr;

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return validatePattern(value, emailRegex, "Invalid email format");
    },

    password: (value) => {
        const requiredErr = validateRequired(value, "Password");
        if (requiredErr) return requiredErr;

        if (value.length < 2) return "Password must be at least 2 characters";
        return null;
    },

    name: (value) => {
        const requiredErr = validateRequired(value, "name");
        if (requiredErr) return requiredErr;

        return validatePattern(value, /^[^<>{}"'`]+$/, "Invalid characters in name");
    },

    phone: (value) => {
        const requiredErr = validateRequired(value, "Phone");
        if (requiredErr) return requiredErr;

        const phoneRegex = /^[0-9+\-()\s]+$/; // allows digits, +, -, (), spaces
        return validatePattern(value, phoneRegex, "Invalid phone number");
    },

    required: validateRequired,
};

export default InputCheck;
