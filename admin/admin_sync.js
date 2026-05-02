import { db } from "./firebase_init.js";
import { collection, getDocs, writeBatch, doc, setDoc } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";

// State for pending items
let pendingItems = [];
let pendingType = ''; // 'exercises', 'meals', or 'articles'

// Pagination state for Sync Center
let syncCurrentPage = 1;
const syncItemsPerPage = 6;

// Helper to log to the sync console
export const log = (msg, type = '') => {
    const c = document.getElementById('syncConsole');
    if (!c) return;
    const entry = document.createElement('div');
    if (type === 'success') entry.style.color = '#10b981';
    if (type === 'err') entry.style.color = '#ef4444';
    entry.innerHTML = `<span class="timestamp">[${new Date().toLocaleTimeString()}]</span> ${msg}`;
    c.appendChild(entry);
    c.scrollTop = c.scrollHeight;
};

window.clearConsole = () => {
    const c = document.getElementById('syncConsole');
    if (c) c.innerHTML = "Console cleared...";
};

// UI: Show Preview Table with Pagination
const showPreviewUI = () => {
    let previewArea = document.getElementById('syncPreviewArea');
    if (!previewArea) {
        previewArea = document.createElement('div');
        previewArea.id = 'syncPreviewArea';
        previewArea.className = 'data-card';
        previewArea.style.marginTop = '30px';
        
        const syncTab = document.getElementById('syncEng');
        if (syncTab) {
            const container = syncTab.querySelector('.sync-grid')?.parentElement || syncTab;
            container.appendChild(previewArea);
        }
    }

    if (pendingItems.length === 0) {
        previewArea.style.display = 'none';
        return;
    }

    previewArea.style.display = 'block';
    
    // Pagination logic
    const totalPages = Math.ceil(pendingItems.length / syncItemsPerPage);
    if (syncCurrentPage > totalPages) syncCurrentPage = Math.max(1, totalPages);

    const startIndex = (syncCurrentPage - 1) * syncItemsPerPage;
    const endIndex = startIndex + syncItemsPerPage;
    const paginatedItems = pendingItems.slice(startIndex, endIndex);

    let html = `
        <div class="table-header">
            <div>
                <h2 style="display: flex; align-items: center; gap: 12px; margin: 0;">
                    <i class="fas fa-clipboard-check" style="color: var(--primary);"></i>
                    Sync Preview: ${pendingType.charAt(0).toUpperCase() + pendingType.slice(1)}
                </h2>
                <p style="color: var(--text-gray); font-size: 14px; margin-top: 4px; font-weight: 500;">
                    Reviewing ${pendingItems.length} items before cloud commitment
                </p>
            </div>
            <div style="display:flex; gap:12px;">
                <button class="btn btn-secondary" onclick="cancelSync()">Discard All</button>
                <button class="btn btn-primary" onclick="commitPendingSync()">
                    <i class="fas fa-cloud-upload-alt"></i> Sync to Database
                </button>
            </div>
        </div>

        <div class="table-wrapper" style="margin-top: 10px;">
            <table>
                <thead>
                    <tr>
                        <th class="col-visual">Visual</th>
                        <th>Name / Title</th>
                        <th>Category / Info</th>
                        <th class="col-actions" style="text-align: center;">Actions</th>
                    </tr>
                </thead>
                <tbody>
    `;

    paginatedItems.forEach((item, pIndex) => {
        const globalIndex = startIndex + pIndex;
        const img = item.gifUrl || item.imageName || 'assets/logo.png';
        const name = item.name || item.title || 'Untitled';
        const detail = item.target || item.mealTime || item.category || 'N/A';
        html += `
            <tr>
                <td class="col-visual">
                    <img src="${img}" class="avatar" style="border: 2px solid #f1f5f9;" onerror="this.src='assets/logo.png'">
                </td>
                <td>
                    <div style="font-weight: 700; color: var(--secondary); font-size: 15px;">${name}</div>
                </td>
                <td>
                    <span class="badge badge-primary" style="background: #f0fdf4; color: #16a34a; border: 1px solid #dcfce7;">
                        ${detail}
                    </span>
                </td>
                <td class="col-actions" style="text-align: center;">
                    <button class="btn btn-danger btn-sm" style="width: 36px; height: 36px; padding: 0;" onclick="removePendingItem(${globalIndex})">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </td>
            </tr>
        `;
    });

    html += `</tbody></table></div>`;

    // Modern Pagination Footer
    html += `
        <div class="pagination" style="margin-top: 32px; padding-top: 20px; border-top: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center;">
            <div style="color: var(--text-gray); font-size: 14px; font-weight: 600;">
                Showing <span style="color: var(--secondary);">${startIndex + 1} - ${Math.min(endIndex, pendingItems.length)}</span> of ${pendingItems.length}
            </div>

            <div style="display: flex; align-items: center; gap: 15px;">
                <button class="btn btn-secondary btn-sm"
                        style="background: white; border: 1px solid #e2e8f0; opacity: ${syncCurrentPage === 1 ? '0.5' : '1'}; cursor: ${syncCurrentPage === 1 ? 'not-allowed' : 'pointer'};"
                        onclick="changeSyncPage(${syncCurrentPage - 1})" ${syncCurrentPage === 1 ? 'disabled' : ''}>
                    <i class="fas fa-arrow-left"></i>
                </button>

                <div style="font-weight: 700; color: var(--secondary); font-size: 14px; background: #f8fafc; padding: 8px 16px; border-radius: 10px;">
                    Page ${syncCurrentPage} of ${totalPages}
                </div>

                <button class="btn btn-secondary btn-sm"
                        style="background: white; border: 1px solid #e2e8f0; opacity: ${syncCurrentPage === totalPages ? '0.5' : '1'}; cursor: ${syncCurrentPage === totalPages ? 'not-allowed' : 'pointer'};"
                        onclick="changeSyncPage(${syncCurrentPage + 1})" ${syncCurrentPage === totalPages ? 'disabled' : ''}>
                    <i class="fas fa-arrow-right"></i>
                </button>
            </div>
        </div>
    `;

    previewArea.innerHTML = html;
};

window.changeSyncPage = (page) => {
    syncCurrentPage = page;
    showPreviewUI();
    const previewArea = document.getElementById('syncPreviewArea');
    if (previewArea) {
        const offset = 100;
        const bodyRect = document.body.getBoundingClientRect().top;
        const elementRect = previewArea.getBoundingClientRect().top;
        const elementPosition = elementRect - bodyRect;
        const offsetPosition = elementPosition - offset;

        window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth'
        });
    }
};

window.removePendingItem = (index) => {
    pendingItems.splice(index, 1);
    if (pendingItems.length === 0) {
        document.getElementById('syncPreviewArea').style.display = 'none';
    } else {
        showPreviewUI();
    }
};

window.cancelSync = () => {
    if(confirm("Discard all fetched items?")) {
        pendingItems = [];
        pendingType = '';
        syncCurrentPage = 1;
        document.getElementById('syncPreviewArea').style.display = 'none';
        log("Sync session discarded.");
    }
};

window.commitPendingSync = async () => {
    if (pendingItems.length === 0) return;

    const countToSync = pendingItems.length;
    log(`⏳ Syncing ${countToSync} items to '${pendingType}' collection...`);

    try {
        let batch = writeBatch(db);
        let count = 0;

        for (const item of pendingItems) {
            const ref = doc(db, pendingType, item.id);
            batch.set(ref, item, { merge: true });
            count++;

            if (count % 25 === 0) {
                await batch.commit();
                log(`Progress: ${count} / ${countToSync}...`, 'success');
                batch = writeBatch(db);
            }
        }

        if (count % 25 !== 0) await batch.commit();

        log(`✅ SUCCESS: ${count} items added to ${pendingType}!`, 'success');
        pendingItems = [];
        pendingType = '';
        syncCurrentPage = 1;
        document.getElementById('syncPreviewArea').style.display = 'none';

        if (window.updateStats) window.updateStats();
    } catch (e) {
        log("❌ Sync Error: " + e.message, 'err');
    }
};

window.startExerciseSync = async () => {
    syncCurrentPage = 1;
    log("🚀 Checking existing exercises in database...");
    let existingNames = new Set();
    try {
        const querySnapshot = await getDocs(collection(db, "exercises"));
        querySnapshot.forEach((doc) => {
            const data = doc.data();
            if (data.name) {
                existingNames.add(data.name.toLowerCase().trim());
            }
        });
        log(`Found ${existingNames.size} existing exercises. Duplicates will be skipped.`);
    } catch (e) {
        log("⚠️ Could not fetch existing exercises, proceeding anyway: " + e.message, "err");
    }

    log("🚀 Fetching Global Exercise CSV...");
    try {
        const res = await fetch("https://raw.githubusercontent.com/azilRababe/Exercises_Dataset/main/data/gifs.csv");
        const text = await res.text();
        const lines = text.split(/\r?\n/);
        if (lines.length < 2) { log("❌ CSV empty or unreachable.", "err"); return; }

        const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
        const rows = lines.slice(1);

        const titleIdx = headers.indexOf('title'), targetIdx = headers.indexOf('targetMuscle'), srcIdx = headers.indexOf('src');
        const finalTitleIdx = titleIdx !== -1 ? titleIdx : (headers.indexOf('name') !== -1 ? headers.indexOf('name') : 1);
        const finalTargetIdx = targetIdx !== -1 ? targetIdx : (headers.indexOf('bodyPart') !== -1 ? headers.indexOf('bodyPart') : 0);
        const finalSrcIdx = srcIdx !== -1 ? srcIdx : (headers.indexOf('gifUrl') !== -1 ? headers.indexOf('gifUrl') : 2);

        pendingItems = [];
        pendingType = 'exercises';
        let skipCount = 0;

        for (const row of rows) {
            if (!row.trim()) continue;
            const cols = []; let curr = '', inQ = false;
            for (let char of row) {
                if (char === '"') inQ = !inQ;
                else if (char === ',' && !inQ) { cols.push(curr.trim()); curr = ''; }
                else curr += char;
            }
            cols.push(curr.trim());

            if (cols.length <= Math.max(finalTitleIdx, finalTargetIdx, finalSrcIdx)) continue;

            const name = cols[finalTitleIdx].replace(/^"|"$/g, ''),
                  target = cols[finalTargetIdx].replace(/^"|"$/g, ''),
                  gifUrl = cols[finalSrcIdx].replace(/^"|"$/g, '');

            if (!name || !gifUrl) continue;

            // Duplicate check
            if (existingNames.has(name.toLowerCase().trim())) {
                skipCount++;
                continue;
            }

            const safeName = name.toLowerCase().replace(/[^a-z0-9]/g, ''),
                  safeTarget = target.toLowerCase().replace(/[^a-z0-9]/g, '');
            const exId = "ex_" + safeName.substring(0, 15) + "_" + safeTarget.substring(0, 10);

            pendingItems.push({
                id: exId,
                name,
                target,
                bodyPart: target,
                gifUrl,
                secondary: "",
                category: "main exercise",
                difficulty: "Intermediate",
                instructions: ["Maintain controlled form", "Focus on target muscle", "Breathe steadily"]
            });
        }

        log(`📋 Preview Ready: ${pendingItems.length} new exercises found. (${skipCount} duplicates skipped)`);
        showPreviewUI();
    } catch (e) { log("❌ CSV Load Error: " + e.message, 'err'); }
};

window.startMealSync = async () => {
    syncCurrentPage = 1;
    log("🚀 Checking existing meals in database...");
    let existingNames = new Set();
    try {
        const querySnapshot = await getDocs(collection(db, "meals"));
        querySnapshot.forEach((doc) => {
            const data = doc.data();
            if (data.name) {
                existingNames.add(data.name.toLowerCase().trim());
            }
        });
        log(`Found ${existingNames.size} existing meals. Duplicates will be skipped.`);
    } catch (e) {
        log("⚠️ Could not fetch existing meals, proceeding anyway: " + e.message, "err");
    }

    log("🍲 Fetching Global Recipes...");
    try {
        // Fetching from a few common categories to get "all" diverse results
        const categories = ["Beef", "Chicken", "Seafood", "Vegetarian", "Pasta", "Pork"];
        pendingItems = [];
        pendingType = 'meals';
        let skipCount = 0;

        for (const cat of categories) {
            log(`Fetching ${cat} category...`);
            const resp = await fetch(`https://www.themealdb.com/api/json/v1/1/filter.php?c=${cat}`);
            const data = await resp.json();

            if(data.meals) {
                // Limit to 10 per category for preview performance
                for(const m of data.meals.slice(0, 10)) {
                    if (existingNames.has(m.strMeal.toLowerCase().trim())) {
                        skipCount++;
                        continue;
                    }

                    const dR = await fetch(`https://www.themealdb.com/api/json/v1/1/lookup.php?i=${m.idMeal}`);
                    const dD = await dR.json();
                    const fM = dD.meals[0];
                    const ing = [];
                    for(let i=1; i<=20; i++) { if(fM[`strIngredient${i}`]) ing.push(fM[`strIngredient${i}`]); }

                    const p = Math.floor(Math.random()*20)+10,
                          c = Math.floor(Math.random()*30)+20,
                          f = Math.floor(Math.random()*15)+5,
                          dur = Math.floor(Math.random()*45)+15;
                    const mTime = ["Breakfast", "Lunch", "Dinner"][Math.floor(Math.random()*3)];

                    pendingItems.push({
                        id: m.idMeal,
                        name: fM.strMeal,
                        imageName: fM.strMealThumb,
                        duration: dur,
                        mealTime: mTime,
                        preferredDiet: "Balanced",
                        macros: {protein:p, carbs:c, fats:f},
                        calories: (p*4 + c*4 + f*9),
                        ingredients: ing,
                        instructions: fM.strInstructions,
                        category: cat
                    });
                }
            }
        }
        log(`📋 Preview Ready: ${pendingItems.length} new recipes found. (${skipCount} duplicates skipped)`);
        showPreviewUI();
    } catch (e) { log("❌ Recipe Fetch Error: " + e.message, 'err'); }
};

window.startNewsSync = async () => {
    syncCurrentPage = 1;
    const token = document.getElementById('newsToken')?.value;
    if(!token) return alert("API Token Required");

    log("🚀 Checking existing articles in database...");
    let existingTitles = new Set();
    try {
        const querySnapshot = await getDocs(collection(db, "articles"));
        querySnapshot.forEach((doc) => {
            const data = doc.data();
            if (data.title) {
                existingTitles.add(data.title.toLowerCase().trim());
            }
        });
        log(`Found ${existingTitles.size} existing articles. Duplicates will be skipped.`);
    } catch (e) {
        log("⚠️ Could not fetch existing articles, proceeding anyway: " + e.message, "err");
    }

    log("📰 Fetching Health & Wellness News...");
    try {
        const q = encodeURIComponent('nutrition OR fitness OR wellness');
        const res = await fetch(`https://newsapi.org/v2/everything?q=${q}&language=en&sortBy=relevancy&pageSize=50&apiKey=${token}`);
        const data = await res.json();

        if (data.status !== "ok") throw new Error(data.message || "NewsAPI Error");

        pendingItems = [];
        pendingType = 'articles';
        let skipCount = 0;

        for (const article of data.articles) {
            if (!article.urlToImage || article.title === "[Removed]") continue;

            if (existingTitles.has(article.title.toLowerCase().trim())) {
                skipCount++;
                continue;
            }

            const id = "art_" + btoa(article.url).replace(/[^a-zA-Z0-9]/g, '').substring(0, 24);
            pendingItems.push({
                id: id,
                title: article.title,
                description: article.description || "",
                content: article.content || "",
                imageName: article.urlToImage,
                articleUrl: article.url,
                author: article.author || "Health Expert",
                source: article.source.name,
                date: article.publishedAt,
                category: "Wellness"
            });
            if (pendingItems.length >= 25) break; // Limit news sync count
        }

        if (pendingItems.length === 0) {
            log(`⚠️ No new relevant articles found. (${skipCount} duplicates skipped)`, "err");
        } else {
            log(`📋 Preview Ready: ${pendingItems.length} new health articles fetched. (${skipCount} duplicates skipped)`);
            showPreviewUI();
        }
    } catch (error) {
        log("❌ News Sync Error: " + error.message, 'err');
    }
};

window.confirmWipe = async (c) => {
    if(confirm("WIPE all data from '" + c + "'?")) {
        log(`🗑️ Wiping collection: ${c}...`);
        try {
            const s = await getDocs(collection(db,c));
            const b = writeBatch(db);
            s.forEach(d=>b.delete(d.ref));
            await b.commit();
            log(`✨ Collection '${c}' wiped.`, 'success');
            if (window.updateStats) window.updateStats();
            if(c==='meals') { if (window.loadMeals) window.loadMeals(); }
            else if(c==='articles') { if (window.loadArticles) window.loadArticles(); }
            else if(c==='exercises') { if (window.loadExercises) window.loadExercises(); }
        } catch (e) {
            log("❌ Wipe failed: " + e.message, 'err');
        }
    } 
};
