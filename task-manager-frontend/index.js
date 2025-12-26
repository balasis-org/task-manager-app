// LOGIN
const loginBtn = document.getElementById('loginBtn');
loginBtn.addEventListener('click', () => {
    fetch('/api/auth/login-url')
        .then(res => res.text())
        .then(url => {
            window.location.href = url;   // redirect to Azure AD
        });
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


document.getElementById("addReviewer").addEventListener("click", () => {
    const input = document.createElement("input");
    input.type = "number";
    input.name = "reviewerIds";
    input.placeholder = "User ID";
    document.getElementById("reviewers").appendChild(input);
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

        const res = await fetch(`api/groups/${groupId}/task/${taskId}`,
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


const managementOutput = document.getElementById("managementOutput");

// ADD ASSIGNEE apu
document.getElementById("addAssigneeForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const userId = data.get("userId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/assignees/${userId}`, { method: "POST" });
        managementOutput.textContent = res.ok ? `Assignee ${userId} added.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// remove assignee api
document.getElementById("removeAssigneeForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const userId = data.get("userId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/assignees/${userId}`, { method: "DELETE" });
        managementOutput.textContent = res.ok ? `Assignee ${userId} removed.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// add reviewer api
document.getElementById("addReviewerForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const userId = data.get("userId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/reviewers/${userId}`, { method: "POST" });
        managementOutput.textContent = res.ok ? `Reviewer ${userId} added.` : `Failed: ${res.status}`;
    } catch (err) {
        managementOutput.textContent = `Error: ${err.message}`;
    }
});

// REMOVE REVIEWER API
document.getElementById("removeReviewerForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const taskId = data.get("taskId");
    const userId = data.get("userId");

    try {
        const res = await fetch(`/api/groups/${groupId}/task/${taskId}/reviewers/${userId}`, { method: "DELETE" });
        managementOutput.textContent = res.ok ? `Reviewer ${userId} removed.` : `Failed: ${res.status}`;
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


const invitationOutput = document.getElementById("invitationOutput");

// Invite user
document.getElementById("inviteUserForm").addEventListener("submit", async e => {
    e.preventDefault();
    const data = new FormData(e.target);
    const groupId = data.get("groupId");
    const userId = data.get("userId");

    try {
        const res = await fetch(`/api/groups/${groupId}/invite`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: Number(userId) })
        });
        const result = await res.json();
        invitationOutput.textContent = res.ok
            ? `Invitation sent:\n${JSON.stringify(result, null, 2)}`
            : `Failed: ${res.status}`;
    } catch (err) {
        invitationOutput.textContent = `Error: ${err.message}`;
    }
});

//
// <button id="findMyInvitesBtn"> Find my invites </button>
// <div id="findMyInvitesOutput">
//
// </div>

// Find my invites
const findMyInvitesOutput = document.getElementById("findMyInvitesOutput");

document.querySelector("#findMyInvitesBtn").addEventListener("click",async ()=>{
    try{
        const res = await fetch("api/group-invitations/me");
        const result = await res.json();
        findMyInvitesOutput.innerHTML = res.ok ? JSON.stringify(result,null,2) : "";
    }catch (err) {
        findMyInvitesOutput.innerHTML = `Error: ${err.message}`;
    }
})




// Accept invitation
document.getElementById("acceptInvitationForm").addEventListener("submit", async e => {
    e.preventDefault();
    const invitationId = e.target.invitationId.value;

    try {
        const res = await fetch(`/api/group-invitations/${invitationId}/accept`, {
            method: "POST"
        });
        const result = await res.json();
        invitationOutput.textContent = res.ok
            ? `Invitation accepted:\n${JSON.stringify(result, null, 2)}`
            : `Failed: ${res.status}`;
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
    const payload = Object.fromEntries(formData.entries());

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

        let html = `
            <h4>Files</h4>
        `;

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

        taskWithFilesOutput.innerHTML = html;

    } catch (err) {
        taskWithFilesOutput.innerHTML = `Failed: ${err.message}`;
    }
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