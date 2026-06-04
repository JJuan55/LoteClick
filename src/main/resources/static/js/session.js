function getActiveUser() {
    const rawUser = sessionStorage.getItem("user");
    if (!rawUser) return null;

    try {
        return JSON.parse(rawUser);
    } catch (e) {
        sessionStorage.clear();
        return null;
    }
}

function getAuthHeaders(extraHeaders = {}) {
    const user = getActiveUser();
    if (!user || !user.token) {
        return extraHeaders;
    }

    return {
        ...extraHeaders,
        "Authorization": `Bearer ${user.token}`
    };
}
