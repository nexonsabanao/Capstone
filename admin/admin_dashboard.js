import { db, auth, firebaseConfig, settings } from "./firebase_init.js";
import { collection, getDocs, doc, getDoc } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";
import { onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-auth.js";
import "./admin_management.js";
import "./admin_sync.js";
import "./admin_report.js";

window.cacheEx = []; window.cacheMl = []; window.cacheArt = []; window.cacheSt = []; 
let charts = {};
let stFilteredData = [], exFilteredData = [], mlFilteredData = [], artFilteredData = [];
let stPage = 1, exPage = 1, mlPage = 1, artPage = 1;
let sortConfig = { field: '', dir: 'asc' };
const limitVal = settings.limitVal || 10;

// Idle Session Management (20 Minutes)
let idleTimer;
const resetIdleTimer = () => {
    clearTimeout(idleTimer);
    idleTimer = setTimeout(async () => {
        console.warn("Session expired due to inactivity.");
        try {
            await signOut(auth);
            window.location.href = 'admin_login.html?error=timeout';
        } catch (err) {
            window.location.href = 'admin_login.html';
        }
    }, 20 * 60 * 1000);
};

function startIdleMonitoring() {
    ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'].forEach(name => {
        document.addEventListener(name, resetIdleTimer, true);
    });
    resetIdleTimer();
}

// Security Guard: Monitor Auth State
onAuthStateChanged(auth, async (user) => {
    if (!user) {
        window.location.href = 'admin_login.html';
        return;
    }

    // Secondary verification: Ensure user exists in 'admins' collection
    try {
        const adminDoc = await getDoc(doc(db, "admins", user.uid));
        if (!adminDoc.exists()) {
            console.warn("Unauthorized access attempt detected.");
            await signOut(auth);
            window.location.href = 'admin_login.html?error=unauthorized';
            return;
        }
        
        // Access Granted: Remove Security Shield
        const shield = document.getElementById('securityShield');
        if (shield) {
            shield.classList.add('hidden');
            setTimeout(() => shield.style.display = 'none', 500);
        }
        document.body.classList.remove('auth-pending');

        // Initialize Dashboard
        initDashboard();
    } catch (error) {
        console.error("Security verification failed:", error);
        window.location.href = 'admin_login.html';
    }
});

// Logout function
window.logoutAdmin = async () => {
    if (confirm("Are you sure you want to sign out?")) {
        try {
            await signOut(auth);
            window.location.href = 'admin_login.html';
        } catch (error) {
            console.error("Logout error:", error);
            alert("Logout failed. Please try again.");
        }
    }
};

function initDashboard() {
    updateStats();
    startIdleMonitoring();
    
    // Global Chart Defaults
    if (typeof Chart !== 'undefined') {
        Chart.defaults.font.family = "'Plus Jakarta Sans', sans-serif";
        Chart.defaults.color = '#64748b';
    }

    setInterval(() => { 
        const timeDisplay = document.getElementById('timeDisplay');
        if (timeDisplay) timeDisplay.innerHTML = `<i class="far fa-clock"></i> ${new Date().toLocaleTimeString()}`; 
    }, 1000);
}

function toggleSidebar(force) {
    const s = document.getElementById('sidebar'), o = document.getElementById('sidebarOverlay');
    if (!s || !o) return;
    const isShowing = s.classList.contains('show');
    const show = force !== undefined ? force : !isShowing;
    
    if (show) { s.classList.add('show'); o.classList.add('show'); }
    else { s.classList.remove('show'); o.classList.remove('show'); }
}
window.toggleSidebar = toggleSidebar;

async function updateStats() {
    try {
        const [uSnap, eSnap, mSnap, aSnap] = await Promise.all([
            getDocs(collection(db,"users")), 
            getDocs(collection(db,"exercises")), 
            getDocs(collection(db,"meals")), 
            getDocs(collection(db,"articles"))
        ]);
        window.cacheSt = uSnap.docs.map(d=>({ ...d.data(), fid: d.id }));
        window.cacheEx = eSnap.docs.map(d=>({ ...d.data(), id: d.id }));
        window.cacheMl = mSnap.docs.map(d=>({ ...d.data(), id: d.id }));
        window.cacheArt = aSnap.docs.map(d=>({ ...d.data(), id: d.id }));

        const st = document.getElementById("statStudents"), ex = document.getElementById("statExercises"), 
              ml = document.getElementById("statMeals"), art = document.getElementById("statArticles");
        
        if (st) st.innerText = window.cacheSt.length;
        if (ex) ex.innerText = window.cacheEx.length;
        if (ml) ml.innerText = window.cacheMl.length;
        if (art) art.innerText = window.cacheArt.length;

        renderCharts(window.cacheSt);
        
        const activeTab = document.querySelector('.tab-content.active')?.id;
        if (activeTab === 'students') { stFilteredData = [...window.cacheSt]; renderStudents(); }
        if (activeTab === 'exercises') { exFilteredData = [...window.cacheEx]; renderExercises(); }
        if (activeTab === 'meals') { mlFilteredData = [...window.cacheMl]; renderMeals(); }
        if (activeTab === 'articleLib') { artFilteredData = [...window.cacheArt]; renderArticles(); }

        const dot = document.getElementById('statusDot'), txt = document.getElementById('statusText');
        if (dot) dot.classList.add('connected'); 
        if (txt) txt.innerText = 'Connected';
    } catch (e) { 
        const dot = document.getElementById('statusDot'), txt = document.getElementById('statusText');
        if (dot) dot.classList.remove('connected'); 
        if (txt) txt.innerText = 'Disconnected';
        console.error("Update stats error:", e);
    }
}
window.updateStats = updateStats;

function renderCharts(users) {
    if (typeof Chart === 'undefined') return;
    Object.values(charts).forEach(c=>c.destroy());

    const activeUsers = users.filter(u => u.status !== 'deleted');

    // 1. Fitness Goals Breakdown (Doughnut)
    const goalStats = {};
    activeUsers.forEach(u => {
        const val = String(u.goal || 'Not Set').trim();
        goalStats[val] = (goalStats[val] || 0) + 1;
    });

    const goalCtx = document.getElementById('goalChart');
    if (goalCtx) {
        charts.goal = new Chart(goalCtx, {
            type:'doughnut',
            data:{
                labels: Object.keys(goalStats),
                datasets:[{
                    data: Object.values(goalStats),
                    backgroundColor: ['#00A78B', '#3b82f6', '#f59e0b', '#ef4444', '#6366f1'],
                    hoverOffset: 20,
                    borderRadius: 10,
                    borderWidth: 4,
                    borderColor: '#ffffff'
                }]
            },
            options:{
                cutout:'70%',
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { usePointStyle: true, padding: 25, font: { weight: '600', size: 12 } }
                    }
                }
            }
        });
    }

    // 2. Weight Distribution (Bar)
    const weights = [0,0,0,0];
    activeUsers.forEach(u => {
        const w = parseFloat(u.weightKg || 0);
        if(w > 0 && w < 50) weights[0]++;
        else if(w >= 50 && w < 70) weights[1]++;
        else if(w >= 70 && w < 90) weights[2]++;
        else if(w >= 90) weights[3]++;
    });

    const weightCtx = document.getElementById('weightDistChart');
    if (weightCtx) {
        charts.weight = new Chart(weightCtx, {
            type:'bar',
            data:{
                labels:['<50kg','50-70kg','70-90kg','>90kg'],
                datasets:[{
                    label:'Users',
                    data:weights,
                    backgroundColor:'rgba(0, 167, 139, 0.7)',
                    hoverBackgroundColor: '#00A78B',
                    borderRadius: 12,
                    barThickness: 50
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { borderDash: [5, 5], color: '#e2e8f0', drawBorder: false } },
                    x: { grid: { display: false } }
                }
            }
        });
    }

    // 3. Diet Preferences (Polar Area)
    const dietStats = {};
    activeUsers.forEach(u => {
        const val = String(u.preferredDiet || 'Not Set').trim();
        dietStats[val] = (dietStats[val] || 0) + 1;
    });

    const dietCtx = document.getElementById('mealDietChart');
    if (dietCtx) {
        charts.diet = new Chart(dietCtx, {
            type:'polarArea',
            data:{
                labels: Object.keys(dietStats),
                datasets:[{
                    data: Object.values(dietStats),
                    backgroundColor: [
                        'rgba(99, 102, 241, 0.7)',
                        'rgba(16, 185, 129, 0.7)',
                        'rgba(244, 63, 94, 0.7)',
                        'rgba(249, 115, 22, 0.7)'
                    ],
                    borderColor: '#ffffff',
                    borderWidth: 2
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { usePointStyle: true, font: { weight: '600' } }
                    }
                },
                scales: { r: { grid: { color: '#e2e8f0' }, ticks: { display: false } } }
            }
        });
    }

    // 4. Training Progress (Line)
    const stages = new Array(29).fill(0);
    activeUsers.forEach(u => {
        const d = parseInt(u.lastCompletedWorkoutDay || 0);
        if(d >= 0 && d <= 28) stages[d]++;
    });

    const stageCtx = document.getElementById('trainingStageChart');
    if (stageCtx) {
        charts.stage = new Chart(stageCtx, {
            type:'line',
            data:{
                labels: Array.from({length: 29}, (_, i) => 'D' + i),
                datasets:[{
                    label:'Users at Stage',
                    data:stages,
                    borderColor:'#00A78B',
                    borderWidth: 3,
                    tension:0.4,
                    fill:true,
                    backgroundColor: (ctx) => {
                        const gradient = ctx.chart.ctx.createLinearGradient(0, 0, 0, 400);
                        gradient.addColorStop(0, 'rgba(0, 167, 139, 0.2)');
                        gradient.addColorStop(1, 'rgba(0, 167, 139, 0)');
                        return gradient;
                    },
                    pointRadius: 3,
                    pointBackgroundColor: '#ffffff',
                    pointBorderColor: '#00A78B',
                    pointBorderWidth: 1,
                    pointHoverRadius: 6
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            title: (items) => `Day ${items[0].label.substring(1)}`
                        }
                    }
                },
                scales: {
                    y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { stepSize: 1 } },
                    x: {
                        grid: { display: false },
                        ticks: {
                            callback: function(val, index) {
                                return index % 7 === 0 ? this.getLabelForValue(val) : '';
                            }
                        }
                    }
                }
            }
        });
    }

    // 5. Activity Level Distribution
    const activityStats = {};
    activeUsers.forEach(u => {
        const val = String(u.activityLevel || 'Not Set').trim();
        activityStats[val] = (activityStats[val] || 0) + 1;
    });

    const activityCtx = document.getElementById('activityChart');
    if (activityCtx) {
        charts.activity = new Chart(activityCtx, {
            type: 'bar',
            data: {
                labels: Object.keys(activityStats),
                datasets: [{
                    label: 'Users',
                    data: Object.values(activityStats),
                    backgroundColor: '#3b82f6',
                    borderRadius: 8,
                    barThickness: 40
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { borderDash: [5, 5], color: '#e2e8f0' } },
                    x: { grid: { display: false } }
                }
            }
        });
    }

    // 6. Exclusion Breakdown
    const exclusionMap = {};
    const foodIcons = {
        'Egg': '🥚', 'Peanut': '🥜', 'Milk': '🥛', 'Dairy': '🥛', 'Soy': '🫘', 'Wheat': '🌾',
        'Fish': '🐟', 'Shellfish': '🦐', 'Nut': '🥜', 'Chicken': '🍗', 'Pork': '🥩', 'Beef': '🥩',
        'Shrimp': '🦐', 'Crab': '🦀', 'Tomato': '🍅', 'Onion': '🧅', 'Garlic': '🧄', 'Sugar': '🍬',
        'Chocolate': '🍫', 'Honey': '🍯', 'Mushroom': '🍄', 'Cheese': '🧀', 'Strawberry': '🍓',
        'Seafood': '🍤', 'Bread': '🍞', 'Pasta': '🍝', 'Nut': '🥜', 'Cashew': '🥜', 'Almond': '🥜'
    };

    activeUsers.forEach(u => {
        let exclusions = u.excludedIngredients || u.exclusions || u.excluded_ingredients || [];
        let rawItems = [];
        if (Array.isArray(exclusions)) rawItems = exclusions;
        else if (typeof exclusions === 'string') rawItems = exclusions.split(',').map(i => i.trim());

        rawItems.forEach(item => {
            let clean = String(item).trim().toLowerCase();
            if (!clean || clean === "none") return;
            if (clean.endsWith('ies') && clean.length > 4) clean = clean.slice(0, -3) + 'y';
            else if (clean.endsWith('es') && (clean.endsWith('oes') || clean.endsWith('ches') || clean.endsWith('shes'))) clean = clean.slice(0, -2);
            else if (clean.endsWith('s') && !clean.endsWith('ss') && clean.length > 3) clean = clean.slice(0, -1);
            const displayLabel = clean.charAt(0).toUpperCase() + clean.slice(1);
            exclusionMap[displayLabel] = (exclusionMap[displayLabel] || 0) + 1;
        });
    });

    const sortedExclusions = Object.entries(exclusionMap).sort((a, b) => b[1] - a[1]).slice(0, 10);
    const exclusionCtx = document.getElementById('exclusionChart');
    if (exclusionCtx) {
        charts.exclusion = new Chart(exclusionCtx, {
            type: 'bar',
            data: {
                labels: sortedExclusions.length > 0 ? sortedExclusions.map(e => `${foodIcons[e[0]] || '🚫'} ${e[0]}`) : ['No exclusions logged'],
                datasets: [{
                    label: 'Users Avoiding',
                    data: sortedExclusions.length > 0 ? sortedExclusions.map(e => e[1]) : [0],
                    backgroundColor: '#f43f5e',
                    borderRadius: 8,
                    barThickness: 25
                }]
            },
            options: {
                indexAxis: 'y',
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { stepSize: 1 } },
                    y: { grid: { display: false } }
                }
            }
        });
    }

    // 7. Gender Distribution
    const genderCounts = { 'Male': 0, 'Female': 0, 'Not Specified': 0 };
    activeUsers.forEach(u => {
        let val = String(u.gender || 'Not Specified').trim();
        if (val !== 'Male' && val !== 'Female') val = 'Not Specified';
        genderCounts[val]++;
    });

    const genderCtx = document.getElementById('genderChart');
    if (genderCtx) {
        charts.gender = new Chart(genderCtx, {
            type: 'bar',
            data: {
                labels: ['Male', 'Female', 'Not Specified'],
                datasets: [{
                    label: 'Users',
                    data: [genderCounts['Male'], genderCounts['Female'], genderCounts['Not Specified']],
                    backgroundColor: ['#0ea5e9', '#ec4899', '#94a3b8'],
                    borderRadius: 10,
                    barThickness: 30
                }]
            },
            options: {
                indexAxis: 'y',
                maintainAspectRatio: false,
                plugins: { 
                    legend: { display: false },
                    tooltip: { callbacks: { label: (context) => ` ${context.raw} Users` } }
                },
                scales: {
                    x: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { stepSize: 1 } },
                    y: { grid: { display: false } }
                }
            }
        });
    }
}

function showDeletedAccountInfo(uid) {
    const msg = `This account has been disabled (Soft Deleted).

To manage this further, please use the Firebase Console:

1. RE-ENABLE ACCOUNT:
Go to your Firestore 'users' collection, find document '${uid}', and change the 'status' field from 'deleted' back to 'active'.

2. PERMANENT DELETE:
First, delete document '${uid}' from your Firestore 'users' collection.
Then, go to the 'Authentication' tab in Firebase Console and delete the user with UID '${uid}'.

This dashboard only handles 'Soft Deletion' to preserve security and app logic.`;
    alert(msg);
}
window.showDeletedAccountInfo = showDeletedAccountInfo;

function renderStudents() {
    const b = document.getElementById("studentBody");
    if (!b) return;
    b.innerHTML = "";
    const start = (stPage - 1) * limitVal, end = start + limitVal;
    stFilteredData.slice(start, end).forEach(u => {
        const isDeleted = u.status === 'deleted';
        const emailFallback = u.email || `ID: ${u.fid || 'Unknown'}`;
        const nameFallback = u.name || "User";
        const rowStyle = isDeleted ? 'style="background: #fff5f5; opacity: 0.8;"' : '';
        const nameDisplay = isDeleted ? `<del>${nameFallback}</del> <span class="badge" style="background:#fee2e2; color:#ef4444; font-size:10px; padding:2px 6px">DELETED</span>` : `<b>${nameFallback}</b>`;
        const actionButtons = isDeleted
            ? `<button class="btn btn-secondary btn-sm" onclick="showDeletedAccountInfo('${u.fid}')">Details</button>`
            : `<div class="action-btns">
               <button class="btn btn-primary btn-sm" title="Download Report" onclick="downloadUserReport('${u.fid}', '${nameFallback}')"><i class="fas fa-file-download"></i></button>
               <button class="btn btn-secondary btn-sm" title="Edit Profile" onclick="openStudentModal('${u.fid}')"><i class="fas fa-edit"></i></button>
               <button class="btn btn-danger btn-sm" title="Delete User" onclick="deleteRecord('users','${u.fid}')"><i class="fas fa-trash-alt"></i></button>
               </div>`;

        b.innerHTML += `<tr ${rowStyle}>
            <td data-label="User">${nameDisplay}<br><small>${emailFallback}</small></td>
            <td data-label="Goal"><span class="badge" style="background:#f1f5f9">${u.goal || 'Not set'}</span></td>
            <td data-label="Activity">${u.activityLevel || 'Not set'}</td>
            <td data-label="Progress" style="color:var(--primary); font-weight:800">Day ${u.lastCompletedWorkoutDay||0}</td>
            <td data-label="Actions">${actionButtons}</td>
        </tr>`;
    });
    const totalPages = Math.ceil(stFilteredData.length / limitVal) || 1;
    const info = document.getElementById('stPageInfo');
    if (info) info.innerText = `Page ${stPage} of ${totalPages}`;
}
window.renderStudents = renderStudents;

function renderExercises() {
    const b = document.getElementById("exerciseBody");
    if (!b) return;
    b.innerHTML = "";
    const start = (exPage - 1) * limitVal, end = start + limitVal;
    exFilteredData.slice(start, end).forEach(e => {
        b.innerHTML += `<tr>
            <td data-label="Visual"><img src="${e.gifUrl}" class="avatar"></td>
            <td data-label="Title">${e.name}</td>
            <td data-label="Target">${e.target}</td>
            <td data-label="Category">${e.category}</td>
            <td data-label="Level">${e.difficulty}</td>
            <td data-label="Actions">
                <div class="action-btns">
                    <button class="btn btn-secondary btn-sm" onclick="openExModal('${e.id}')"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('exercises','${e.id}')"><i class="fas fa-trash-alt"></i></button>
                </div>
            </td>
        </tr>`;
    });
    const totalPages = Math.ceil(exFilteredData.length / limitVal) || 1;
    const info = document.getElementById('exPageInfo');
    if (info) info.innerText = `Page ${exPage} of ${totalPages}`;
}
window.renderExercises = renderExercises;

function renderMeals() {
    const b = document.getElementById("mealBody");
    if (!b) return;
    b.innerHTML = "";
    const start = (mlPage - 1) * limitVal, end = start + limitVal;
    mlFilteredData.slice(start, end).forEach(m => {
        b.innerHTML += `<tr>
            <td data-label="Img"><img src="${m.imageName}" class="avatar"></td>
            <td data-label="Recipe">${m.name}</td>
            <td data-label="Time">${m.mealTime}</td>
            <td data-label="Cals">${m.calories}</td>
            <td data-label="Diet">${m.preferredDiet}</td>
            <td data-label="Macros">P:${m.macros?.protein}g</td>
            <td data-label="Actions">
                <div class="action-btns">
                    <button class="btn btn-secondary btn-sm" onclick="openMealModal('${m.id}')"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('meals','${m.id}')"><i class="fas fa-trash-alt"></i></button>
                </div>
            </td>
        </tr>`;
    });
    const totalPages = Math.ceil(mlFilteredData.length / limitVal) || 1;
    const info = document.getElementById('mealPageInfo');
    if (info) info.innerText = `Page ${mlPage} of ${totalPages}`;
}
window.renderMeals = renderMeals;

function renderArticles() {
    const b = document.getElementById("articleBody");
    if (!b) return;
    b.innerHTML = "";
    const start = (artPage - 1) * limitVal, end = start + limitVal;
    artFilteredData.slice(start, end).forEach(a => {
        b.innerHTML += `<tr>
            <td data-label="Preview"><img src="${a.imageName}" class="avatar"></td>
            <td data-label="Headline">${(a.title || '').substring(0,30)}...</td>
            <td data-label="Author">${a.author}</td>
            <td data-label="Category">${a.category}</td>
            <td data-label="Actions">
                <div class="action-btns">
                    <button class="btn btn-secondary btn-sm" onclick="openArtModal('${a.id}')"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('articles','${a.id}')"><i class="fas fa-trash-alt"></i></button>
                </div>
            </td>
        </tr>`;
    });
    const totalPages = Math.ceil(artFilteredData.length / limitVal) || 1;
    const info = document.getElementById('artPageInfo');
    if (info) info.innerText = `Page ${artPage} of ${totalPages}`;
}
window.renderArticles = renderArticles;

function sortData(category, field) {
    let data, renderFn;
    if (category === 'exercises') { data = exFilteredData; renderFn = renderExercises; }
    else if (category === 'meals') { data = mlFilteredData; renderFn = renderMeals; }
    else if (category === 'articleLib') { data = artFilteredData; renderFn = renderArticles; }
    else if (category === 'students') { data = stFilteredData; renderFn = renderStudents; }
    if (!data) return;
    if (sortConfig.field === field) sortConfig.dir = sortConfig.dir === 'asc' ? 'desc' : 'asc';
    else { sortConfig.field = field; sortConfig.dir = 'asc'; }
    data.sort((a, b) => {
        let valA = a[field] ?? '', valB = b[field] ?? '';
        if (typeof valA === 'string') valA = valA.toLowerCase();
        if (typeof valB === 'string') valB = valB.toLowerCase();
        if (valA < valB) return sortConfig.dir === 'asc' ? -1 : 1;
        if (valA > valB) return sortConfig.dir === 'asc' ? 1 : -1;
        return 0;
    });
    renderFn();
}
window.sortData = sortData;

function handleSearch(type) {
    const q = document.getElementById(type+'Search').value.toLowerCase();
    if(type === 'st') { stFilteredData = window.cacheSt.filter(u => (u.name||'').toLowerCase().includes(q) || (u.email||'').toLowerCase().includes(q)); stPage = 1; renderStudents(); }
    else if(type === 'ex') { exFilteredData = window.cacheEx.filter(e => (e.name||'').toLowerCase().includes(q) || (e.target||'').toLowerCase().includes(q)); exPage = 1; renderExercises(); }
    else if(type === 'meal') { mlFilteredData = window.cacheMl.filter(m => (m.name||'').toLowerCase().includes(q) || (m.preferredDiet||'').toLowerCase().includes(q)); mlPage = 1; renderMeals(); }
    else if(type === 'art') { artFilteredData = window.cacheArt.filter(a => (a.title||'').toLowerCase().includes(q) || (a.category||'').toLowerCase().includes(q)); artPage = 1; renderArticles(); }
}
window.handleSearch = handleSearch;

function changePage(type, dir) {
    if(type==='st') { if(dir==='next' && stPage < Math.ceil(stFilteredData.length/limitVal)) stPage++; else if(dir==='prev' && stPage > 1) stPage--; renderStudents(); }
    else if(type==='ex') { if(dir==='next' && exPage < Math.ceil(exFilteredData.length/limitVal)) exPage++; else if(dir==='prev' && exPage > 1) exPage--; renderExercises(); }
    else if(type==='meal') { if(dir==='next' && mlPage < Math.ceil(mlFilteredData.length/limitVal)) mlPage++; else if(dir==='prev' && mlPage > 1) mlPage--; renderMeals(); }
    else if(type==='art') { if(dir==='next' && artPage < Math.ceil(artFilteredData.length/limitVal)) artPage++; else if(dir==='prev' && artPage > 1) artPage--; renderArticles(); }
}
window.changePage = changePage;

async function loadStudents() { if(!window.cacheSt.length) await updateStats(); stFilteredData = [...window.cacheSt]; renderStudents(); }
window.loadStudents = loadStudents;

async function loadExercises() { if(!window.cacheEx.length) await updateStats(); exFilteredData = [...window.cacheEx]; renderExercises(); }
window.loadExercises = loadExercises;

async function loadMeals() { if(!window.cacheMl.length) await updateStats(); mlFilteredData = [...window.cacheMl]; renderMeals(); }
window.loadMeals = loadMeals;

async function loadArticles() { if(!window.cacheArt.length) await updateStats(); artFilteredData = [...window.cacheArt]; renderArticles(); }
window.loadArticles = loadArticles;

function switchTab(t) {
    document.querySelectorAll('.tab-content').forEach(x=>x.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(x=>x.classList.remove('active'));
    const target = document.getElementById(t);
    if (target) target.classList.add('active');
    const mapping = { overview: 'Overview', students: 'Users', exercises: 'Exercises', meals: 'Meal Library', articleLib: 'Article Hub', syncEng: 'Sync Center' };
    Array.from(document.querySelectorAll('.nav-item')).find(x => x.textContent.trim().includes(mapping[t]))?.classList.add('active');
    const title = document.getElementById('tabTitle');
    if (title) title.innerText = mapping[t];
    if(t==='students') loadStudents(); if(t==='exercises') loadExercises(); if(t==='meals') loadMeals(); if(t==='articleLib') loadArticles(); if(t==='overview') updateStats();
}
window.switchTab = switchTab;
