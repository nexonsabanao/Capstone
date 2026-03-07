import { db, settings } from "./firebase_init.js";
import { collection, getDocs } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";
import "./admin_management.js";
import "./admin_sync.js";

window.cacheEx = []; window.cacheMl = []; window.cacheArt = []; window.cacheSt = []; 
let charts = {};
let stFilteredData = [], exFilteredData = [], mlFilteredData = [], artFilteredData = [];
let stPage = 1, exPage = 1, mlPage = 1, artPage = 1;
let sortConfig = { field: '', dir: 'asc' };
const limitVal = settings.limitVal || 10;

function toggleSidebar(force) {
    const s = document.getElementById('sidebar'), o = document.getElementById('sidebarOverlay');
    if (!s || !o) return;
    const isShowing = s.classList.contains('show');
    const show = force !== undefined ? force : !isShowing;
    
    if (show) { s.classList.add('show'); o.classList.add('show'); }
    else { s.classList.remove('show'); o.classList.remove('show'); }
}
window.toggleSidebar = toggleSidebar;

setInterval(() => { 
    const timeDisplay = document.getElementById('timeDisplay');
    if (timeDisplay) timeDisplay.innerHTML = `<i class="far fa-clock"></i> ${new Date().toLocaleTimeString()}`; 
}, 1000);

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

    const goalStats = {};
    users.forEach(u => {
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
                    backgroundColor: ['#ef4444', '#3b82f6', '#f59e0b', '#10b981', '#6366f1', '#f43f5e']
                }]
            },
            options:{ cutout:'70%', plugins: { legend: { position: 'bottom' } } }
        });
    }

    const weights = [0,0,0,0];
    users.forEach(u => { 
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
                datasets:[{label:'Students', data:weights, backgroundColor:'#10b981'}] 
            }, 
            options: { plugins: { legend: { display: false } } } 
        });
    }

    const dietStats = {};
    users.forEach(u => {
        const val = String(u.preferredDiet || 'Not Set').trim();
        dietStats[val] = (dietStats[val] || 0) + 1;
    });

    const dietCtx = document.getElementById('mealDietChart');
    if (dietCtx) {
        charts.diet = new Chart(dietCtx, {
            type:'pie',
            data:{
                labels: Object.keys(dietStats),
                datasets:[{
                    data: Object.values(dietStats),
                    backgroundColor: ['#6366f1', '#10b981', '#f43f5e', '#ef4444', '#3b82f6', '#f59e0b']
                }]
            },
            options: { plugins: { legend: { position: 'bottom' } } }
        });
    }

    const stages = [0,0,0,0,0,0,0,0];
    users.forEach(u => { 
        const d = parseInt(u.lastCompletedWorkoutDay || 0); 
        if(d >= 0 && d < 8) stages[d]++; 
    });
    
    const stageCtx = document.getElementById('trainingStageChart');
    if (stageCtx) {
        charts.stage = new Chart(stageCtx, { 
            type:'line', 
            data:{ 
                labels:['D0','D1','D2','D3','D4','D5','D6','D7'], 
                datasets:[{label:'Progress Level', data:stages, borderColor:'#00A78B', tension:0.4, fill:true, backgroundColor:'rgba(0,167,139,0.1)'}] 
            } 
        });
    }
}

function renderStudents() {
    const b = document.getElementById("studentBody"); 
    if (!b) return;
    b.innerHTML = "";
    const start = (stPage - 1) * limitVal, end = start + limitVal;
    stFilteredData.slice(start, end).forEach(u => {
        // Use fid (Firestore ID) as fallback for missing email
        const emailFallback = u.email || `ID: ${u.fid || 'Unknown'}`;
        const nameFallback = u.name || "User";
        
        b.innerHTML += `<tr>
            <td data-label="Student"><b>${nameFallback}</b><br><small>${emailFallback}</small></td>
            <td data-label="Goal"><span class="badge" style="background:#f1f5f9">${u.goal || 'Not set'}</span></td>
            <td data-label="Activity">${u.activityLevel || 'Not set'}</td>
            <td data-label="Progress" style="color:var(--primary); font-weight:800">Day ${u.lastCompletedWorkoutDay||0}</td>
            <td data-label="Actions">
                <button class="btn btn-edit btn-sm" onclick="openStudentModal('${u.fid}')">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="deleteRecord('users','${u.fid}')">Delete</button>
            </td>
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
                <button class="btn btn-secondary btn-sm" onclick="openExModal('${e.id}')">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="deleteRecord('exercises','${e.id}')">Delete</button>
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
                <button class="btn btn-secondary btn-sm" onclick="openMealModal('${m.id}')">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="deleteRecord('meals','${m.id}')">Delete</button>
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
                <button class="btn btn-secondary btn-sm" onclick="openArtModal('${a.id}')">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="deleteRecord('articles','${a.id}')">Delete</button>
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

    if (sortConfig.field === field) {
        sortConfig.dir = sortConfig.dir === 'asc' ? 'desc' : 'asc';
    } else {
        sortConfig.field = field;
        sortConfig.dir = 'asc';
    }

    data.sort((a, b) => {
        let valA = a[field] ?? '';
        let valB = b[field] ?? '';
        
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
    if(type === 'st') { 
        stFilteredData = window.cacheSt.filter(u => (u.name||'').toLowerCase().includes(q) || (u.email||'').toLowerCase().includes(q)); 
        stPage = 1; renderStudents(); 
    } else if(type === 'ex') {
        exFilteredData = window.cacheEx.filter(e => (e.name||'').toLowerCase().includes(q) || (e.target||'').toLowerCase().includes(q));
        exPage = 1; renderExercises();
    } else if(type === 'meal') {
        mlFilteredData = window.cacheMl.filter(m => (m.name||'').toLowerCase().includes(q) || (m.preferredDiet||'').toLowerCase().includes(q));
        mlPage = 1; renderMeals();
    } else if(type === 'art') {
        artFilteredData = window.cacheArt.filter(a => (a.title||'').toLowerCase().includes(q) || (a.category||'').toLowerCase().includes(q));
        artPage = 1; renderArticles();
    }
}
window.handleSearch = handleSearch;

function changePage(type, dir) {
    if(type==='st') { 
        if(dir==='next' && stPage < Math.ceil(stFilteredData.length/limitVal)) stPage++; 
        else if(dir==='prev' && stPage > 1) stPage--; 
        renderStudents(); 
    } else if(type==='ex') {
        if(dir==='next' && exPage < Math.ceil(exFilteredData.length/limitVal)) exPage++; 
        else if(dir==='prev' && exPage > 1) exPage--; 
        renderExercises();
    } else if(type==='meal') {
        if(dir==='next' && mlPage < Math.ceil(mlFilteredData.length/limitVal)) mlPage++; 
        else if(dir==='prev' && mlPage > 1) mlPage--; 
        renderMeals();
    } else if(type==='art') {
        if(dir==='next' && artPage < Math.ceil(artFilteredData.length/limitVal)) artPage++; 
        else if(dir==='prev' && artPage > 1) artPage--; 
        renderArticles();
    }
}
window.changePage = changePage;

async function loadStudents() { 
    if(!window.cacheSt.length) await updateStats(); 
    stFilteredData = [...window.cacheSt]; 
    renderStudents(); 
}
window.loadStudents = loadStudents;

async function loadExercises() { 
    if(!window.cacheEx.length) await updateStats(); 
    exFilteredData = [...window.cacheEx]; 
    renderExercises(); 
}
window.loadExercises = loadExercises;

async function loadMeals() { 
    if(!window.cacheMl.length) await updateStats(); 
    mlFilteredData = [...window.cacheMl]; 
    renderMeals(); 
}
window.loadMeals = loadMeals;

async function loadArticles() { 
    if(!window.cacheArt.length) await updateStats(); 
    artFilteredData = [...window.cacheArt]; 
    renderArticles(); 
}
window.loadArticles = loadArticles;

function switchTab(t) {
    document.querySelectorAll('.tab-content').forEach(x=>x.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(x=>x.classList.remove('active'));
    const target = document.getElementById(t);
    if (target) target.classList.add('active');
    
    const mapping = { overview: 'Overview', students: 'Students', exercises: 'Exercises', meals: 'Meal Library', articleLib: 'Article Hub', syncEng: 'Sync Center' };
    Array.from(document.querySelectorAll('.nav-item')).find(x => x.textContent.trim().includes(mapping[t]))?.classList.add('active');
    
    const title = document.getElementById('tabTitle');
    if (title) title.innerText = mapping[t];
    
    if(t==='students') loadStudents(); 
    if(t==='exercises') loadExercises(); 
    if(t==='meals') loadMeals(); 
    if(t==='articleLib') loadArticles(); 
    if(t==='overview') updateStats();
}
window.switchTab = switchTab;

// INITIALIZATION
updateStats();
