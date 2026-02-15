// LOGIN
const loginBtn = document.getElementById('loginBtn');
loginBtn.addEventListener('click', () => {
    fetch('/api/auth/login-url')
        .then(res => res.text())
        .then(url => {
            window.location.href = url;   // redirect to Azure AD
        });
});

// FAKE LOGIN (dev)
const fakeLoginBtn = document.getElementById('fakeLoginBtn');
const fakeUserSelect = document.getElementById('fakeUserSelect');

fakeLoginBtn.addEventListener('click', async () => {
    const email = fakeUserSelect.value;
    const name = fakeUserSelect.options[fakeUserSelect.selectedIndex]?.text || email;

    try {
        const res = await fetch('/api/auth/fake-login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, name })
        });

        const text = await res.text();
        outputDiv.innerHTML = res.ok ? text : `Fake login failed: ${res.status}\n${text}`;

        if (res.ok) {
            // Cookies are HttpOnly; reload so subsequent calls are authenticated.
            window.location.reload();
        }
    } catch (err) {
        outputDiv.innerHTML = 'Error: ' + err.message;
    }
});

// LOGOUT
const logoutBtn = document.getElementById('logoutBtn');
logoutBtn.addEventListener('click', () => {
    fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
        .then(response => {
            if (response.ok) {
                outputDiv.innerHTML = 'Logged out successfully.';

                window.location.reload();
            } else {
                outputDiv.innerHTML = 'Logout failed.';
            }
        })
        .catch(err => {
            outputDiv.innerHTML = 'Error: ' + err.message;
        });
});


// ---- Smoke button guns (small chained automations) ----
const smokeCtx = {
    userId: null,
    groupId: null,
    taskId: null
};

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function setPre(preEl, text) {
    preEl.textContent = String(text ?? "");
}

function appendPre(preEl, line) {
    preEl.textContent = (preEl.textContent ? preEl.textContent + "\n" : "") + String(line);
}

async function fetchTextOrThrow(url, options) {
    const res = await fetch(url, options);
    const text = await res.text();
    if (!res.ok) {
        const err = new Error(text || `HTTP ${res.status}`);
        err.status = res.status;
        throw err;
    }
    return text;
}

async function fetchJsonOrThrow(url, options) {
    const res = await fetch(url, options);
    const text = await res.text();
    if (!res.ok) {
        const err = new Error(text || `HTTP ${res.status}`);
        err.status = res.status;
        throw err;
    }
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

async function getMyProfile() {
    return await fetchJsonOrThrow("/api/users/me");
}

async function fakeLoginSilently() {
    const email = fakeUserSelect.value;
    const name = fakeUserSelect.options[fakeUserSelect.selectedIndex]?.text || email;

    await fetchTextOrThrow("/api/auth/fake-login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ email, name })
    });
}

async function ensureLoggedIn(preEl) {
    try {
        const profile = await getMyProfile();
        smokeCtx.userId = profile?.id ?? smokeCtx.userId;
        return profile;
    } catch (err) {
        if (err && (err.status === 401 || err.status === 403)) {
            appendPre(preEl, "Not logged in; attempting dev fake-login...");
            await fakeLoginSilently();
            await sleep(300);
            const profile = await getMyProfile();
            smokeCtx.userId = profile?.id ?? smokeCtx.userId;
            return profile;
        }
        throw err;
    }
}

async function createSmokeGroup() {
    const payload = {
        name: `Smoke group ${new Date().toISOString()}`,
        description: "Smoke test group",
        Announcement: "",
        allowEmailNotification: true
    };
    return await fetchJsonOrThrow("/api/groups", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });
}

async function createSmokeTask(groupId, userId) {
    const task = {
        title: `Smoke task ${new Date().toISOString()}`,
        description: "Smoke test task",
        taskState: "TODO",
        assignedIds: [Number(userId)],
        reviewerIds: [Number(userId)]
    };

    const form = new FormData();
    form.append(
        "data",
        new Blob([JSON.stringify(task)], { type: "application/json" })
    );

    return await fetchJsonOrThrow(`/api/groups/${groupId}/tasks`, {
        method: "POST",
        body: form
    });
}

async function addSmokeComment(groupId, taskId) {
    return await fetchJsonOrThrow(`/api/groups/${groupId}/task/${taskId}/comments`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ comment: `Smoke comment ${new Date().toISOString()}` })
    });
}

async function markToBeReviewed(groupId, taskId) {
    return await fetchJsonOrThrow(`/api/groups/${groupId}/task/${taskId}/to-be-reviewed`, {
        method: "POST"
    });
}

async function reviewTask(groupId, taskId) {
    return await fetchJsonOrThrow(`/api/groups/${groupId}/task/${taskId}/review`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reviewersDecision: "APPROVE", reviewComment: "Smoke approve" })
    });
}

async function fetchEvents(groupId) {
    return await fetchJsonOrThrow(`/api/groups/${groupId}/events?page=0&size=5`);
}

async function fetchGroupPreview(groupId) {
    return await fetchJsonOrThrow(`/api/groups/${groupId}`);
}

// Smoke A
const smokeAuthProfileGroupsBtn = document.getElementById("smokeAuthProfileGroupsBtn");
const smokeAuthProfileGroupsOutput = document.getElementById("smokeAuthProfileGroupsOutput");

smokeAuthProfileGroupsBtn.addEventListener("click", async () => {
    setPre(smokeAuthProfileGroupsOutput, "Running Smoke A...");
    try {
        const profile = await ensureLoggedIn(smokeAuthProfileGroupsOutput);
        appendPre(smokeAuthProfileGroupsOutput, `Profile OK (id=${profile?.id ?? "?"}).`);

        const groups = await fetchJsonOrThrow("/api/groups");
        appendPre(smokeAuthProfileGroupsOutput, `Groups: ${Array.isArray(groups) ? groups.length : "(non-array)"}`);
        appendPre(smokeAuthProfileGroupsOutput, JSON.stringify(groups, null, 2));
    } catch (err) {
        appendPre(smokeAuthProfileGroupsOutput, `Error: ${err.message}`);
    }
});

// Smoke B
const smokeCreateGroupAndTaskBtn = document.getElementById("smokeCreateGroupAndTaskBtn");
const smokeCreateGroupAndTaskOutput = document.getElementById("smokeCreateGroupAndTaskOutput");

smokeCreateGroupAndTaskBtn.addEventListener("click", async () => {
    setPre(smokeCreateGroupAndTaskOutput, "Running Smoke B...");
    try {
        const profile = await ensureLoggedIn(smokeCreateGroupAndTaskOutput);
        const userId = profile?.id;
        if (!userId && userId !== 0) throw new Error("/api/users/me did not return an id");

        appendPre(smokeCreateGroupAndTaskOutput, "Creating group...");
        const group = await createSmokeGroup();
        const groupId = group?.id;
        smokeCtx.groupId = groupId ?? smokeCtx.groupId;

        appendPre(smokeCreateGroupAndTaskOutput, `Group created (id=${groupId ?? "?"}).`);
        appendPre(smokeCreateGroupAndTaskOutput, JSON.stringify(group, null, 2));

        appendPre(smokeCreateGroupAndTaskOutput, "Waiting 1.5s...");
        await sleep(1500);

        if (!groupId && groupId !== 0) throw new Error("Create group response did not include id; cannot create task");

        appendPre(smokeCreateGroupAndTaskOutput, "Creating task (assigned+reviewer = me)...");
        const task = await createSmokeTask(groupId, userId);
        const taskId = task?.id;
        smokeCtx.taskId = taskId ?? smokeCtx.taskId;
        smokeCtx.userId = userId;

        appendPre(smokeCreateGroupAndTaskOutput, `Task created (id=${taskId ?? "?"}).`);
        appendPre(smokeCreateGroupAndTaskOutput, JSON.stringify(task, null, 2));
        appendPre(smokeCreateGroupAndTaskOutput, `Saved smokeCtx: groupId=${smokeCtx.groupId}, taskId=${smokeCtx.taskId}`);
    } catch (err) {
        appendPre(smokeCreateGroupAndTaskOutput, `Error: ${err.message}`);
    }
});

// Smoke C
const smokeCommentReviewEventsBtn = document.getElementById("smokeCommentReviewEventsBtn");
const smokeCommentReviewEventsOutput = document.getElementById("smokeCommentReviewEventsOutput");

smokeCommentReviewEventsBtn.addEventListener("click", async () => {
    setPre(smokeCommentReviewEventsOutput, "Running Smoke C...");
    try {
        await ensureLoggedIn(smokeCommentReviewEventsOutput);

        const groupId = smokeCtx.groupId;
        const taskId = smokeCtx.taskId;
        if (!groupId && groupId !== 0) throw new Error("No smokeCtx.groupId yet. Run Smoke B first.");
        if (!taskId && taskId !== 0) throw new Error("No smokeCtx.taskId yet. Run Smoke B first.");

        appendPre(smokeCommentReviewEventsOutput, `Using groupId=${groupId}, taskId=${taskId}`);

        appendPre(smokeCommentReviewEventsOutput, "Adding comment...");
        const comment = await addSmokeComment(groupId, taskId);
        appendPre(smokeCommentReviewEventsOutput, JSON.stringify(comment, null, 2));

        appendPre(smokeCommentReviewEventsOutput, "Marking TO_BE_REVIEWED...");
        const toBeReviewed = await markToBeReviewed(groupId, taskId);
        appendPre(smokeCommentReviewEventsOutput, JSON.stringify(toBeReviewed, null, 2));

        appendPre(smokeCommentReviewEventsOutput, "Waiting 0.5s...");
        await sleep(500);

        appendPre(smokeCommentReviewEventsOutput, "Reviewing (APPROVE)...");
        const reviewed = await reviewTask(groupId, taskId);
        appendPre(smokeCommentReviewEventsOutput, JSON.stringify(reviewed, null, 2));

        appendPre(smokeCommentReviewEventsOutput, "Fetching events (page=0,size=5)...");
        const events = await fetchEvents(groupId);
        appendPre(smokeCommentReviewEventsOutput, JSON.stringify(events, null, 2));

        appendPre(smokeCommentReviewEventsOutput, "Fetching group preview...");
        const preview = await fetchGroupPreview(groupId);
        appendPre(smokeCommentReviewEventsOutput, JSON.stringify(preview, null, 2));
    } catch (err) {
        appendPre(smokeCommentReviewEventsOutput, `Error: ${err.message}`);
    }
});

//FIND GROUPS OF LOGGED IN USER
const fetchBtn = document.getElementById('fetchBtn');
const outputDiv = document.getElementById('output');
fetchBtn.addEventListener('click', () => {
    fetch('/api/groups')
        .then(response => {
            if (!response.ok) throw new Error('Network error');
            return response.json();
        })
        .then(data => {
            outputDiv.innerHTML = JSON.stringify(data, null, 2);
        })
        .catch(err => {
            outputDiv.innerHTML = 'Error: ' + err.message;
        });
});

//CLEAR RESULTS OF GROUPS OF LOGGED IN USER
const clearBtn = document.getElementById('clearBtn');
clearBtn.addEventListener('click', () => {
    outputDiv.innerHTML = '';
});

// Get group (with preview tasks)
const getGroupPreviewForm = document.getElementById("getGroupPreviewForm");
const groupPreviewOutput = document.getElementById("groupPreviewOutput");

getGroupPreviewForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(getGroupPreviewForm);
    const groupId = data.get("groupId");

    groupPreviewOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}`);
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        groupPreviewOutput.textContent = JSON.stringify(result, null, 2);
    } catch (err) {
        groupPreviewOutput.textContent = `Error: ${err.message}`;
    }
});

// Group events
const getGroupEventsForm = document.getElementById("getGroupEventsForm");
const groupEventsOutput = document.getElementById("groupEventsOutput");

getGroupEventsForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(getGroupEventsForm);
    const groupId = data.get("groupId");
    const page = data.get("page") ?? "0";
    const size = data.get("size") ?? "10";

    const params = new URLSearchParams();
    if (page !== null && page !== "") params.append("page", String(page));
    if (size !== null && size !== "") params.append("size", String(size));

    groupEventsOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/events?${params.toString()}`);
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        groupEventsOutput.textContent = JSON.stringify(result, null, 2);
    } catch (err) {
        groupEventsOutput.textContent = `Error: ${err.message}`;
    }
});

//CREATE GROUP
const groupFormBtn = document.querySelector("#createGroupBtn");
const groupForm = document.querySelector("#createGroupForm");
groupFormBtn.addEventListener('click',async (event)=>{
    event.preventDefault();
    const json =JSON.stringify(Object.fromEntries(new FormData(groupForm).entries()));
    const res = await fetch('/api/groups',{
        method:'POST',
        headers:{
            "Content-Type":"application/json"
        },
        body:json
    })
    if (res.ok){
        const result = await res.json()
        console.log("Group created",result)
    }else{
        console.error("Failed to create group:", res.status);
    }
});

// PATCH GROUP
const updateGroupForm = document.getElementById("updateGroupForm");
updateGroupForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const formData = Object.fromEntries(new FormData(updateGroupForm).entries());
    const groupId = formData.groupId;
    delete formData.groupId; // remove ID from payload

    try {
        const res = await fetch(`/api/groups/${groupId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(formData)
        });

        if (res.ok) {
            const result = await res.json();
            alert("Group patched: " + JSON.stringify(result, null, 2));
        } else {
            alert("Failed to patch group: " + res.status);
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
});

//DELETE GROUP
const deleteGroupForm = document.getElementById("deleteGroupForm");
deleteGroupForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const groupId = deleteGroupForm.groupId.value;

    try {
        const res = await fetch(`/api/groups/${groupId}`, { method: "DELETE" });

        if (res.ok) {
            alert(`Group ${groupId} deleted successfully.`);
        } else {
            alert("Failed to delete group: " + res.status);
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
});



const createTaskForm = document.getElementById("createTaskForm");


document.getElementById("addAssignee").addEventListener("click", () => {
    const input = document.createElement("input");
    input.type = "number";
    input.name = "assignedIds";
    input.placeholder = "User ID";
    document.getElementById("assignees").appendChild(input);
});


createTaskForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const groupId = document.getElementById("groupIdInput").value;
    const formData = new FormData();
    const taskJson = {
        assignedIds: [],
        reviewerIds: []
    };

    for (const [key, value] of new FormData(createTaskForm).entries()) {
        if (!value) continue;

        if (key === "assignedIds") {
            taskJson.assignedIds.push(Number(value));
        } else if (key === "reviewerIds") {
            taskJson.reviewerIds.push(Number(value));
        } else if (key !== "files") {
            taskJson[key] = value;
        }
    }

    formData.append(
        "data",
        new Blob([JSON.stringify(taskJson)], { type: "application/json" })
    );

    const fileInput = createTaskForm.querySelector('input[name="files"]');
    for (const file of fileInput.files) {
        formData.append("files", file, file.name);
    }

    try {
        const res = await fetch(`/api/groups/${groupId}/tasks`, {
            method: "POST",
            body: formData
        });

        if (res.ok) {
            const result = await res.json();
            alert("Task created:" + JSON.stringify(result));
        } else {
            console.error("Failed to create task", res.status);
        }
    } catch (err) {
        console.error("Error:", err);
    }
});

//Patch Task
const patchForm = document.getElementById("patchTaskForm");
const formDataToBeSend = new FormData();
document.getElementById("patchTaskBtn").addEventListener("click",
    async (e) =>{
        e.preventDefault();
        for ( [key,value] of new FormData(patchForm).entries() ){
            if(value && value.trim() !== ""){
                formDataToBeSend.append(key,value);
            }
        }
        if (!(formDataToBeSend.has("groupId") && formDataToBeSend.has("taskId"))){
            return alert("requires group and task id");
        }
        let groupId = formDataToBeSend.get("groupId");
        let taskId = formDataToBeSend.get("taskId");

        const res = await fetch(`/api/groups/${groupId}/task/${taskId}`,
            {
                method :"PATCH",
                headers: { "Content-Type":"application/json"},
                body :JSON.stringify( Object.fromEntries(formDataToBeSend.entries()))
            }
        )

        if (res.ok) {
            const result = await res.json();
            alert("Task patched:" + JSON.stringify(result));
        } else {
            console.error("Failed to patch task", res.status);
        }

    } ) ;


//get task
const fetchTaskForm = document.getElementById("fetchTaskForm");
const taskOutput = document.getElementById("taskOutput");

fetchTaskForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(fetchTaskForm);
    const groupId = formData.get("groupId");
    const taskId = formData.get("taskId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}`);
        if (!res.ok) throw new Error(`Error ${res.status}`);

        const task = await res.json();
        taskOutput.textContent = JSON.stringify(task, null, 2);
    } catch (err) {
        taskOutput.textContent = `Failed to fetch task: ${err.message}`;
    }
});

// Mark task to-be-reviewed
const markToBeReviewedForm = document.getElementById("markToBeReviewedForm");
const markToBeReviewedOutput = document.getElementById("markToBeReviewedOutput");

markToBeReviewedForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(markToBeReviewedForm);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");

    markToBeReviewedOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/to-be-reviewed`, {
            method: "POST"
        });
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        markToBeReviewedOutput.textContent = JSON.stringify(result, null, 2);
    } catch (err) {
        markToBeReviewedOutput.textContent = `Error: ${err.message}`;
    }
});

// Review task
const reviewTaskForm = document.getElementById("reviewTaskForm");
const reviewTaskOutput = document.getElementById("reviewTaskOutput");

reviewTaskForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(reviewTaskForm);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const reviewersDecision = data.get("reviewersDecision");
    const reviewComment = data.get("reviewComment");

    const payload = {
        reviewersDecision,
        reviewComment: reviewComment && String(reviewComment).trim() !== "" ? String(reviewComment).trim() : null
    };

    reviewTaskOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/review`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        reviewTaskOutput.textContent = JSON.stringify(result, null, 2);
    } catch (err) {
        reviewTaskOutput.textContent = `Error: ${err.message}`;
    }
});

//findMyTasks api
const fetchMyTasksForm = document.getElementById("fetchMyTasksForm");
const myTasksOutput = document.getElementById("myTasksOutput");

fetchMyTasksForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(fetchMyTasksForm);
    const groupId = formData.get("groupId");
    const reviewer = formData.get("reviewer") === "on" ? "true" : null;
    const assigned = formData.get("assigned") === "on" ? "true" : null;
    const taskState = formData.get("taskState") || null;

    const params = new URLSearchParams();
    if (reviewer) params.append("reviewer", reviewer);
    if (assigned) params.append("assigned", assigned);
    if (taskState) params.append("taskState", taskState);

    try {
        const res = await fetch(`/api/groups/${groupId}/task?${params.toString()}`);
        if (!res.ok) throw new Error(`Error ${res.status}`);

        const tasks = await res.json();
        myTasksOutput.textContent = JSON.stringify(tasks, null, 2);
    } catch (err) {
        myTasksOutput.textContent = `Failed to fetch tasks: ${err.message}`;
    }
});


// Task preview search with filters
const searchTasksWithFiltersForm = document.getElementById("searchTasksWithFiltersForm");
const searchTasksWithFiltersOutput = document.getElementById("searchTasksWithFiltersOutput");

searchTasksWithFiltersForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(searchTasksWithFiltersForm);
    const groupId = data.get("groupId");

    const params = new URLSearchParams();

    const creatorId = data.get("creatorId");
    const reviewerId = data.get("reviewerId");
    const assigneeId = data.get("assigneeId");

    if (creatorId) params.append("creatorId", creatorId);
    if (data.get("creatorIsMe") === "on") params.append("creatorIsMe", "true");

    if (reviewerId) params.append("reviewerId", reviewerId);
    if (data.get("reviewerIsMe") === "on") params.append("reviewerIsMe", "true");

    if (assigneeId) params.append("assigneeId", assigneeId);
    if (data.get("assigneeIsMe") === "on") params.append("assigneeIsMe", "true");

    const dueDateBeforeRaw = data.get("dueDateBefore");
    if (dueDateBeforeRaw) {
        const iso = new Date(dueDateBeforeRaw).toISOString();
        params.append("dueDateBefore", iso);
    }

    try {
        const res = await fetch(`/api/groups/${groupId}/tasks/search?${params.toString()}`);
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        searchTasksWithFiltersOutput.textContent = JSON.stringify(result, null, 2);
    } catch (err) {
        searchTasksWithFiltersOutput.textContent = `Error: ${err.message}`;
    }
});


const managementOutput = document.getElementById("managementOutput");

// ADD Participant
document.getElementById("addParticipantForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const userId = data.get("userId");
    const taskParticipantRole = data.get("taskParticipantRole")

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/taskParticipants`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                userId: userId,
                taskParticipantRole: taskParticipantRole
            })
        });
        managementOutput.textContent = res.ok ? `TaskParticipant with userId: ${userId} added.` :
            `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// remove Participant
document.getElementById("removeParticipantForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const taskParticipantId = data.get("taskParticipantId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/taskParticipant/${taskParticipantId}`, { method: "DELETE" });
        managementOutput.textContent = res.ok ? `Participant ${taskParticipantId} removed.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});


// ADD TASK FILE API
document.getElementById("addTaskFileForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/files`, {
            method: "POST",
            body: data
        });
        managementOutput.textContent = res.ok ? `File uploaded.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// ADD ASSIGNEE TASK FILE API
document.getElementById("addAssigneeTaskFileForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/assignee-files`, {
            method: "POST",
            body: data
        });
        managementOutput.textContent = res.ok ? `Assignee file uploaded.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// remove task file api
document.getElementById("removeTaskFileForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const fileId = data.get("fileId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/files/${fileId}`, { method: "DELETE" });
        managementOutput.textContent = res.ok ? `File ${fileId} removed.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// remove assignee task file api
document.getElementById("removeAssigneeTaskFileForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const fileId = data.get("fileId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}`, { method: "DELETE" });
        managementOutput.textContent = res.ok ? `Assignee file ${fileId} removed.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});


// Task comments
const taskCommentsOutput = document.getElementById("taskCommentsOutput");

document.getElementById("addTaskCommentForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const comment = data.get("comment");

    taskCommentsOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ comment: String(comment) })
        });
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        taskCommentsOutput.textContent = `Comment added:\n${JSON.stringify(result, null, 2)}`;
    } catch (err) {
        taskCommentsOutput.textContent = `Error: ${err.message}`;
    }
});

document.getElementById("listTaskCommentsForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const page = data.get("page") ?? "0";
    const size = data.get("size") ?? "10";

    const params = new URLSearchParams();
    if (page !== null && page !== "") params.append("page", String(page));
    if (size !== null && size !== "") params.append("size", String(size));

    taskCommentsOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/comments?${params.toString()}`);
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        taskCommentsOutput.textContent = JSON.stringify(result, null, 2);
    } catch (err) {
        taskCommentsOutput.textContent = `Error: ${err.message}`;
    }
});

document.getElementById("patchTaskCommentForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const commentId = data.get("commentId");
    const comment = data.get("comment");

    taskCommentsOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/comments/${commentId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ comment: String(comment) })
        });
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        taskCommentsOutput.textContent = `Comment patched:\n${JSON.stringify(result, null, 2)}`;
    } catch (err) {
        taskCommentsOutput.textContent = `Error: ${err.message}`;
    }
});

document.getElementById("deleteTaskCommentForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const commentId = data.get("commentId");

    taskCommentsOutput.textContent = "Loading...";
    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/comments/${commentId}`, {
            method: "DELETE"
        });
        if (!res.ok) throw new Error(await res.text());
        taskCommentsOutput.textContent = `Comment ${commentId} deleted.`;
    } catch (err) {
        taskCommentsOutput.textContent = `Error: ${err.message}`;
    }
});


const invitationOutput = document.getElementById("invitationOutput");

// Invite user
document.getElementById("inviteUserForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const userId = data.get("userId");
    const userToBeInvitedRole = data.get("userToBeInvitedRole")
    const comment = data.get("comment");
    console.log(userToBeInvitedRole);

    try {
        const res = await fetch(`/api/groups/${groupId}/invite`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                userId: Number(userId),
                userToBeInvitedRole: userToBeInvitedRole,
                comment: comment && comment.trim() !== "" ? comment.trim() : null
            })
        });
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        invitationOutput.textContent = res.ok
            ? `Invitation sent:\n${JSON.stringify(result, null, 2)}`
            : ``;
    } catch (err) {
        invitationOutput.textContent = `Error: ${err.message}`;
    }
});


// Find my invites
const findMyInvitesOutput = document.getElementById("findMyInvitesOutput");

document.querySelector("#findMyInvitesBtn").addEventListener("click",async ()=>{
    try {
        // Important: /group-invitations/me marks invites as seen (updates lastSeenInvites).
        // To compute "unread" we need the value *before* fetching invites.
        const profileRes = await fetch("/api/users/me");
        if (!profileRes.ok) throw new Error(await profileRes.text());
        const profile = await profileRes.json();
        const lastSeenInvitesBeforeFetch = profile?.lastSeenInvites ? new Date(profile.lastSeenInvites) : null;

        const res = await fetch("/api/group-invitations/me");
        if (!res.ok) throw new Error(await res.text());
        const invites = await res.json();

        const escapeHtml = (s) => String(s)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");

        if (!Array.isArray(invites) || invites.length === 0) {
            findMyInvitesOutput.innerHTML = "(none)";
            return;
        }

        findMyInvitesOutput.innerHTML = invites.map(inv => {
            const id = inv?.id;
            const groupName = inv?.groupName ?? "";
            const invitedByName = inv?.invitedBy?.name ?? "";
            const invitedByEmail = inv?.invitedBy?.email ?? "";
            const role = inv?.userToBeInvitedRole ?? "";
            const comment = inv?.comment ?? "";
            const createdAt = inv?.createdAt ?? "";

            let unread = false;
            if (createdAt && lastSeenInvitesBeforeFetch instanceof Date && !isNaN(lastSeenInvitesBeforeFetch)) {
                const createdAtDate = new Date(createdAt);
                if (!isNaN(createdAtDate)) {
                    unread = createdAtDate > lastSeenInvitesBeforeFetch;
                }
            }

            return `
<div style="border:1px solid #ddd; padding:8px; margin-bottom:8px;">
  <div style="display:flex; justify-content:space-between; align-items:center;">
    <div><strong>ID</strong>: ${escapeHtml(id)}</div>
    ${unread ? '<div style="font-weight:bold;">UNREAD</div>' : ''}
  </div>
  <div><strong>Group</strong>: ${escapeHtml(groupName)}</div>
  <div><strong>From</strong>: ${escapeHtml(invitedByName)} ${invitedByEmail ? `(${escapeHtml(invitedByEmail)})` : ""}</div>
  <div><strong>Role</strong>: ${escapeHtml(role)}</div>
  <div><strong>Created</strong>: ${escapeHtml(createdAt)}</div>
  <div><strong>Comment</strong>: ${escapeHtml(comment)}</div>
</div>`;
        }).join("");
    } catch (err) {
        findMyInvitesOutput.innerHTML = `Error: ${err.message}`;
    }
})


// Find invites I sent
const findMySentInvitesOutput = document.getElementById("findMySentInvitesOutput");

async function cancelInvitationById(invitationId) {
    const res = await fetch(`/api/group-invitations/${invitationId}`, { method: "DELETE" });
    if (!res.ok) {
        throw new Error(await res.text());
    }
}

function renderSentInvites(invites) {
    if (!Array.isArray(invites) || invites.length === 0) {
        findMySentInvitesOutput.innerHTML = "(none)";
        return;
    }

    const escapeHtml = (s) => String(s)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");

    findMySentInvitesOutput.innerHTML = invites.map(inv => {
        const id = inv?.id;
        const groupName = inv?.groupName ?? "";
        const userName = inv?.user?.name ?? "";
        const userEmail = inv?.user?.email ?? "";
        const role = inv?.userToBeInvitedRole ?? "";
        const comment = inv?.comment ?? "";
        const createdAt = inv?.createdAt ?? "";
        return `
<div style="border:1px solid #ddd; padding:8px; margin-bottom:8px;">
  <div><strong>ID</strong>: ${escapeHtml(id)}</div>
  <div><strong>Group</strong>: ${escapeHtml(groupName)}</div>
  <div><strong>To</strong>: ${escapeHtml(userName)} ${userEmail ? `(${escapeHtml(userEmail)})` : ""}</div>
  <div><strong>Role</strong>: ${escapeHtml(role)}</div>
  <div><strong>Created</strong>: ${escapeHtml(createdAt)}</div>
  <div><strong>Comment</strong>: ${escapeHtml(comment)}</div>
  <button type="button" data-cancel-invite-id="${escapeHtml(id)}">Cancel</button>
</div>`;
    }).join("");

    // Attach handlers
    findMySentInvitesOutput.querySelectorAll("button[data-cancel-invite-id]").forEach(btn => {
        btn.addEventListener("click", async () => {
            const id = btn.getAttribute("data-cancel-invite-id");
            try {
                await cancelInvitationById(id);
                btn.textContent = "Canceled";
                btn.disabled = true;
            } catch (err) {
                alert(`Cancel failed: ${err.message}`);
            }
        });
    });
}

document.querySelector("#findMySentInvitesBtn").addEventListener("click", async () => {
    try {
        const res = await fetch("/api/group-invitations/sent");
        const result = await res.json();
        if (!res.ok) throw new Error(JSON.stringify(result));
        renderSentInvites(result);
    } catch (err) {
        findMySentInvitesOutput.innerHTML = `Error: ${err.message}`;
    }
});


// Cancel invite (manual)
document.getElementById("cancelSentInvitationForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const invitationId = e.target.invitationId.value;
    try {
        await cancelInvitationById(invitationId);
        alert(`Invitation ${invitationId} canceled.`);
    } catch (err) {
        alert(`Cancel failed: ${err.message}`);
    }
});




// Accept invitation
document.getElementById("acceptInvitationForm").addEventListener("submit", async e => {
    e.preventDefault();
    const invitationId = e.target.invitationId.value;
    const decision = e.target.decision.value; // ACCEPTED or DECLINED

    try {
        const res = await fetch(`/api/group-invitations/${invitationId}/status?status=${decision}`, {
            method: "PATCH"
        });
        const result = await res.json();
        invitationOutput.textContent = res.ok
            ? `Invitation processed:\n${JSON.stringify(result, null, 2)}`
            : `Failed: ${res.status} ${res.statusText}`;
    } catch (err) {
        invitationOutput.textContent = `Error: ${err.message}`;
    }
});


// fetch my profile (user)
const fetchProfileBtn = document.getElementById("fetchProfileBtn");
const profileOutput = document.getElementById("profileOutput");

fetchProfileBtn.addEventListener("click", async () => {
    try {
        const res = await fetch("/api/users/me");
        if (!res.ok) throw new Error(`Error ${res.status}`);
        const profile = await res.json();
        profileOutput.textContent = JSON.stringify(profile, null, 2);
    } catch (err) {
        profileOutput.textContent = `Failed to fetch profile: ${err.message}`;
    }
});

// edit my profile (only name so far)
const editProfileForm = document.getElementById("editProfileForm");
const editProfileOutput = document.getElementById("editProfileOutput");

editProfileForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const formData = new FormData(editProfileForm);
    const payload = {};
    for (const [key, value] of formData.entries()) {
        if (value === null || value === undefined) continue;
        if (typeof value === "string" && value.trim() === "") continue;
        payload[key] = value;
    }

    try {
        const res = await fetch("/api/users/me", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error(`Error ${res.status}`);
        const updatedProfile = await res.json();
        editProfileOutput.textContent = `Profile updated:\n${JSON.stringify(updatedProfile, null, 2)}`;
    } catch (err) {
        editProfileOutput.textContent = `Failed to update profile: ${err.message}`;
    }
});

//fetch task and show its taskfiles with viewable linkable files
const fetchTaskWithFilesForm = document.getElementById("fetchTaskWithFilesForm");
const taskWithFilesOutput = document.getElementById("taskWithFilesOutput");

fetchTaskWithFilesForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(fetchTaskWithFilesForm);
    const groupId = formData.get("groupId");
    const taskId = formData.get("taskId");

    taskWithFilesOutput.innerHTML = "Loading...";

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}`);
        if (!res.ok) throw new Error(`Error ${res.status}`);

        const task = await res.json();

        let html = `<h4>Files</h4>`;

        if (!task.files || task.files.length === 0) {
            html += `<p>No files attached.</p>`;
        } else {
            html += `<ul>`;
            task.files.forEach(file => {
                html += `
                    <li>
                        ${file.name}
                        —
                        <a href="/api/groups/${groupId}/task/${taskId}/files/${file.id}/download">
                            Download
                        </a>
                    </li>
                `;
            });
            html += `</ul>`;
        }

        html += `<h4>Assignee Files</h4>`;

        if (!task.assigneeFiles || task.assigneeFiles.length === 0) {
            html += `<p>No assignee files attached.</p>`;
        } else {
            html += `<ul>`;
            task.assigneeFiles.forEach(file => {
                html += `
                    <li>
                        ${file.name}
                        —
                        <a href="/api/groups/${groupId}/task/${taskId}/assignee-files/${file.id}/download">
                            Download
                        </a>
                    </li>
                `;
            });
            html += `</ul>`;
        }

        taskWithFilesOutput.innerHTML = html;

    } catch (err) {
        taskWithFilesOutput.innerHTML = `Failed: ${err.message}`;
    }
});


// download assignee task file by id
const downloadAssigneeFileForm = document.getElementById("downloadAssigneeFileForm");
const downloadAssigneeFileOutput = document.getElementById("downloadAssigneeFileOutput");

downloadAssigneeFileForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(downloadAssigneeFileForm);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const fileId = data.get("fileId");

    const url = `/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}/download`;
    downloadAssigneeFileOutput.textContent = `Opening: ${url}`;

    // Use a navigation-based download to preserve Content-Disposition filename.
    window.open(url, "_blank");
});


//upload profile image
const updateProfileImageForm = document.getElementById("updateProfileImageForm");
const profileImageOutput = document.getElementById("profileImageOutput");

updateProfileImageForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const formData = new FormData(updateProfileImageForm);

    try {
        const res = await fetch("/api/users/me/profile-image", {
            method: "POST",
            body: formData
        });

        if (!res.ok) throw new Error(`Error ${res.status}`);
        const updatedProfile = await res.json();

        profileImageOutput.textContent = `Profile image updated:\n${JSON.stringify(updatedProfile, null, 2)}`;
    } catch (err) {
        profileImageOutput.textContent = `Failed to update profile image: ${err.message}`;
    }
});


// Upload group image
const updateGroupImageForm = document.getElementById("updateGroupImageForm");
const groupImageOutput = document.getElementById("groupImageOutput");

updateGroupImageForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const formData = new FormData(updateGroupImageForm);
    const groupId = formData.get("groupId");

    try {
        const res = await fetch(`/api/groups/${groupId}/image`, {
            method: "POST",
            body: formData
        });

        if (!res.ok) throw new Error(`Error ${res.status}`);
        const updatedGroup = await res.json();

        groupImageOutput.textContent = `Group image updated:\n${JSON.stringify(updatedGroup, null, 2)}`;
    } catch (err) {
        groupImageOutput.textContent = `Failed to update group image: ${err.message}`;
    }
});

// get group members
const membershipsForm = document.getElementById("fetchMembershipsForm");
const membershipsOutput = document.getElementById("membershipsOutput");

membershipsForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(membershipsForm);
    const groupId = formData.get("groupId");
    const page = formData.get("page") || 0;
    const size = formData.get("size") || 10;

    try {
        const res = await fetch(`/api/groups/${groupId}/groupMemberships?page=${page}&size=${size}`);
        if (!res.ok) throw new Error(`Error ${res.status}`);

        const data = await res.json();

        // Pretty-print page info + content
        const output = {
            page: data.number,
            size: data.size,
            totalElements: data.totalElements,
            totalPages: data.totalPages,
            content: data.content
        };

        membershipsOutput.textContent = JSON.stringify(output, null, 2);
    } catch (err) {
        membershipsOutput.textContent = `Failed: ${err.message}`;
    }
});


// search group members
const searchMembershipsForm = document.getElementById("searchMembershipsForm");
const searchMembershipsOutput = document.getElementById("searchMembershipsOutput");

searchMembershipsForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(searchMembershipsForm);
    const groupId = formData.get("groupId");
    const q = formData.get("q") || "";
    const page = formData.get("page") || 0;
    const size = formData.get("size") || 10;

    const params = new URLSearchParams();
    if (q && q.trim() !== "") params.append("q", q);
    params.append("page", page);
    params.append("size", size);

    try {
        const res = await fetch(`/api/groups/${groupId}/groupMemberships/search?${params.toString()}`);
        if (!res.ok) throw new Error(`Error ${res.status}`);

        const data = await res.json();

        const output = {
            page: data.number,
            size: data.size,
            totalElements: data.totalElements,
            totalPages: data.totalPages,
            content: data.content
        };

        searchMembershipsOutput.textContent = JSON.stringify(output, null, 2);
    } catch (err) {
        searchMembershipsOutput.textContent = `Failed: ${err.message}`;
    }
});


// remove group members
document.getElementById("removeMembershipForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const groupMembershipId = data.get("groupMembershipId");

    try {
        const res = await fetch(`/api/groups/${groupId}/groupMembership/${groupMembershipId}`, {
            method: "DELETE"
        });

        managementOutput.textContent = res.ok ?
            `GroupMembership ${groupMembershipId} removed.` :
            `Failed to remove: ${res.status} ${res.statusText}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message || err}`;
    }
});




// change membership role
document.getElementById("changeMembershipRoleForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const groupMembershipId = data.get("groupMembershipId");
    const role = data.get("role");

    try {
        const res = await fetch(`/api/groups/${groupId}/groupMembership/${groupMembershipId}/role?role=${role}`, {
            method: "PATCH"
        });
        const result = await res.json();
        managementOutput.textContent = res.ok ? `Role updated:\n${JSON.stringify(result, null, 2)}`
            : ``;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message || err}`;
    }
});
