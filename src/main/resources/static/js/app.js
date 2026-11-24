"use strict";

const API_BASE = "/api";

async function apiRequest(url, options = {}) {
    const response = await fetch(API_BASE + url, {
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json")
        ? await response.json()
        : null;

    if (!response.ok) {
        const error = new Error(data?.message || "Something went wrong");
        error.status = response.status;
        error.details = data?.errors || {};
        throw error;
    }

    return data;
}

function showMessage(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) element.textContent = message || "";
}

function clearFormErrors() {
    ["nameError", "emailError", "phoneError", "passwordError", "errorMessage"]
        .forEach(id => showMessage(id, ""));
}

function showApiError(error) {
    clearFormErrors();

    if (error.details && Object.keys(error.details).length > 0) {
        Object.entries(error.details).forEach(([field, message]) => {
            showMessage(field + "Error", message);
        });
    } else {
        showMessage("errorMessage", error.message);
    }
}

function getFormValues() {
    return {
        name: document.getElementById("name")?.value.trim() || "",
        email: document.getElementById("email")?.value.trim() || "",
        phone: document.getElementById("phone")?.value.trim() || "",
        password: document.getElementById("password")?.value || ""
    };
}

async function initializeLoginPage() {
    const form = document.getElementById("loginForm");
    if (!form) return;

    const registrationMessage = sessionStorage.getItem("registrationMessage");
    if (registrationMessage) {
        showMessage("successMessage", registrationMessage);
        sessionStorage.removeItem("registrationMessage");
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearFormErrors();

        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;

        try {
            await apiRequest("/auth/login", {
                method: "POST",
                body: JSON.stringify({ email, password })
            });
            window.location.href = "/users.html";
        } catch (error) {
            showApiError(error);
        }
    });
}

async function initializeRegisterPage() {
    const form = document.getElementById("registerForm");
    if (!form) return;

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearFormErrors();

        const user = getFormValues();

        try {
            await apiRequest("/auth/register", {
                method: "POST",
                body: JSON.stringify(user)
            });

            sessionStorage.setItem(
                "registrationMessage",
                "Registration successful. Please log in."
            );
            window.location.href = "/login.html";
        } catch (error) {
            showApiError(error);
        }
    });
}

async function requireLogin() {
    try {
        return await apiRequest("/auth/me", { method: "GET" });
    } catch (error) {
        if (error.status === 401) {
            window.location.href = "/login.html";
            return null;
        }
        throw error;
    }
}

async function initializeDashboard() {
    const tableBody = document.getElementById("userTableBody");
    if (!tableBody) return;

    try {
        const currentUser = await requireLogin();
        if (!currentUser) return;

        showMessage("currentUserName", "Welcome, " + currentUser.name);

        const users = await apiRequest("/users", { method: "GET" });
        tableBody.replaceChildren();

        if (!users.length) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 5;
            cell.className = "empty";
            cell.textContent = "No users found.";
            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        users.forEach(user => {
            const row = document.createElement("tr");

            [user.id, user.name, user.email, user.phone].forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value ?? "";
                row.appendChild(cell);
            });

            const actionsCell = document.createElement("td");

            const actionWrap = document.createElement("div");
            actionWrap.className = "action-buttons";

            const editLink = document.createElement("a");
            editLink.className = "btn small";
            editLink.href = "/edit-user.html?id=" + encodeURIComponent(user.id);
            editLink.textContent = "Edit";

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "btn danger small delete-btn";
            deleteButton.dataset.id = user.id;
            deleteButton.textContent = "Delete";

            actionWrap.append(editLink, deleteButton);
            actionsCell.appendChild(actionWrap);
            row.appendChild(actionsCell);
            tableBody.appendChild(row);
        });
    } catch (error) {
        showMessage("errorMessage", error.message);
        if (error.status === 401) window.location.href = "/login.html";
    }
}

async function initializeAddUserPage() {
    const form = document.getElementById("addUserForm");
    if (!form) return;

    const currentUser = await requireLogin().catch(error => {
        showMessage("errorMessage", error.message);
        return null;
    });
    if (!currentUser) return;

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearFormErrors();

        try {
            await apiRequest("/users", {
                method: "POST",
                body: JSON.stringify(getFormValues())
            });
            window.location.href = "/users.html";
        } catch (error) {
            showApiError(error);
        }
    });
}

async function initializeEditUserPage() {
    const form = document.getElementById("editUserForm");
    if (!form) return;

    const currentUser = await requireLogin().catch(error => {
        showMessage("errorMessage", error.message);
        return null;
    });
    if (!currentUser) return;

    const userId = new URLSearchParams(window.location.search).get("id");
    if (!userId || !/^\d+$/.test(userId)) {
        showMessage("errorMessage", "A valid user ID was not provided.");
        form.hidden = true;
        return;
    }

    try {
        const user = await apiRequest("/users/" + encodeURIComponent(userId), {
            method: "GET"
        });
        document.getElementById("name").value = user.name;
        document.getElementById("email").value = user.email;
        document.getElementById("phone").value = user.phone;
    } catch (error) {
        showMessage("errorMessage", error.message);
        form.hidden = true;
        return;
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearFormErrors();

        const user = getFormValues();
        if (!user.password) delete user.password;

        try {
            await apiRequest("/users/" + encodeURIComponent(userId), {
                method: "PUT",
                body: JSON.stringify(user)
            });
            window.location.href = "/users.html";
        } catch (error) {
            showApiError(error);
        }
    });
}

function initializeDeleteButton() {
    const tableBody = document.getElementById("userTableBody");
    if (!tableBody) return;

    tableBody.addEventListener("click", async event => {
        const button = event.target.closest(".delete-btn");
        if (!button) return;

        const userId = button.dataset.id;
        if (!window.confirm("Are you sure you want to delete this user?")) {
            return;
        }

        button.disabled = true;
        try {
            const result = await apiRequest("/users/" + encodeURIComponent(userId), {
                method: "DELETE"
            });
            showMessage("successMessage", result.message);
            await initializeDashboard();
        } catch (error) {
            showMessage("errorMessage", error.message);
            button.disabled = false;
        }
    });
}

function initializeLogout() {
    const logoutButton = document.getElementById("logoutBtn");
    if (!logoutButton) return;

    logoutButton.addEventListener("click", async event => {
        event.preventDefault();
        try {
            await apiRequest("/auth/logout", { method: "POST" });
        } finally {
            window.location.href = "/login.html";
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    initializeLoginPage();
    initializeRegisterPage();
    initializeDashboard();
    initializeAddUserPage();
    initializeEditUserPage();
    initializeDeleteButton();
    initializeLogout();
});
