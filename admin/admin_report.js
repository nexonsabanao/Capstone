import { db } from "./firebase_init.js";
import { collection, getDocs, doc, getDoc } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";

const asNum = (val) => (typeof val === 'number' ? val : parseFloat(val) || 0);

/**
 * Robust Date Helper: Converts Timestamp, Number, or String to JS Date
 */
const toJsDate = (val) => {
    if (!val) return new Date(0);
    if (typeof val.toDate === 'function') return val.toDate(); // Handle Firestore Timestamp
    if (val.seconds !== undefined) return new Date(val.seconds * 1000); // Handle raw Timestamp objects
    return new Date(val); // Handle numbers/strings
};

/**
 * Generates a Professional, Styled HTML Report for Print/PDF
 */
export async function downloadUserReport(uid, name) {
    try {
        console.log(`[REPORT] Fetching data for: ${name}`);

        const [sessionSnap, mealSnap, recordSnap, userDoc] = await Promise.all([
            getDocs(collection(db, "users", uid, "session_history")),
            getDocs(collection(db, "users", uid, "daily_meal_logs")),
            getDocs(collection(db, "users", uid, "exercise_records")),
            getDoc(doc(db, "users", uid))
        ]);

        const u = userDoc.data() || {};
        
        // 1. Process Sessions (History) and calculate real-time totals
        const sessions = [];
        let calculatedCals = 0;
        let calculatedMins = 0;
        
        sessionSnap.forEach(doc => {
            const s = doc.data();
            sessions.push(s);
            // Sum up data from individual sessions
            calculatedCals += asNum(s.caloriesBurned);
            calculatedMins += Math.floor(asNum(s.durationSeconds) / 60);
        });
        
        sessions.sort((a, b) => toJsDate(b.date).getTime() - toJsDate(a.date).getTime());

        // Use the higher value between document totals and calculated totals (safety net)
        const finalCals = Math.max(asNum(u.totalCaloriesBurned), calculatedCals);
        const finalMins = Math.max(asNum(u.totalWorkoutMinutes), calculatedMins);

        // 2. Process Meals (Nutrition)
        const meals = [];
        mealSnap.forEach(doc => meals.push(doc.data()));
        meals.sort((a, b) => toJsDate(b.date).getTime() - toJsDate(a.date).getTime());

        // 3. Process Exercise Records (Performance)
        const records = [];
        recordSnap.forEach(doc => records.push(doc.data()));
        records.sort((a, b) => toJsDate(b.date).getTime() - toJsDate(a.date).getTime());

        // Open a new tab for the report
        const reportWindow = window.open('', '_blank');
        
        if (!reportWindow) {
            alert("Popup Blocked! Please allow popups for this site to view the report.");
            return;
        }

        const htmlContent = `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>User Progress Report - ${name}</title>
            <link rel="icon" type="image/png" href="assets/logo.png">
            <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
            <style>
                :root { --primary: #00A78B; --bg: #ffffff; --text: #1e293b; --muted: #64748b; --border: #e2e8f0; }
                * { box-sizing: border-box; }
                body { font-family: 'Plus Jakarta Sans', sans-serif; color: var(--text); line-height: 1.6; padding: 50px; background: var(--bg); }
                
                .header-container { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid var(--primary); padding-bottom: 20px; margin-bottom: 40px; }
                .brand { display: flex; align-items: center; gap: 20px; }
                .brand img { height: 60px; width: auto; }
                .brand h1 { color: var(--primary); margin: 0; font-size: 32px; font-weight: 800; }
                .user-info { margin-top: 4px; font-size: 15px; }
                .user-info b { color: var(--primary); }

                .summary-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 15px; margin-bottom: 40px; }
                .stat-card { background: #f8fafc; padding: 15px; border-radius: 16px; border: 1px solid var(--border); text-align: center; }
                .stat-card label { display: block; font-size: 10px; font-weight: 800; color: var(--muted); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 5px; }
                .stat-card div { font-size: 18px; font-weight: 800; color: #0f172a; }

                .section-header { display: flex; align-items: center; gap: 12px; margin: 45px 0 20px 0; padding: 10px 15px; background: #f0fdfa; border-left: 6px solid var(--primary); border-radius: 4px; }
                .section-header h2 { font-size: 18px; font-weight: 800; margin: 0; color: #0d9488; text-transform: uppercase; }

                table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                th { text-align: left; padding: 14px; background: #f1f5f9; color: var(--muted); font-size: 12px; font-weight: 700; text-transform: uppercase; border-bottom: 2px solid var(--border); }
                td { padding: 14px; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
                tr:last-child td { border-bottom: none; }
                .badge { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 700; background: #e2e8f0; }

                .empty-msg { text-align: center; padding: 30px; color: var(--muted); background: #f8fafc; border: 2px dashed var(--border); border-radius: 12px; font-style: italic; }
                .footer { margin-top: 60px; text-align: center; font-size: 12px; color: var(--muted); border-top: 1px solid var(--border); padding-top: 20px; }

                @media print {
                    .no-print { display: none !important; }
                    body { padding: 0; }
                    .stat-card { border: 1px solid #ddd; }
                }

                .print-btn { background: var(--primary); color: white; padding: 12px 25px; border-radius: 10px; border: none; font-weight: 700; cursor: pointer; transition: 0.2s; box-shadow: 0 4px 6px rgba(0,167,139,0.2); }
                .print-btn:hover { background: #008f72; transform: translateY(-2px); }
            </style>
        </head>
        <body>
            <div class="header-container">
                <div class="brand">
                    <img src="assets/logo.png" alt="Nutriority Logo" onerror="this.style.display='none'">
                    <div>
                        <h1>Nutriority</h1>
                        <div class="user-info">Report for <b>${u.name || name}</b> | Email: ${u.email || 'N/A'}</div>
                    </div>
                </div>
                <div style="text-align: right">
                    <button class="print-btn no-print" onclick="window.print()">Save as PDF / Print</button>
                    <div style="font-size: 12px; color: var(--muted); margin-top: 10px;">Created: ${new Date().toLocaleDateString()}</div>
                </div>
            </div>

            <div class="summary-row">
                <div class="stat-card"><label>Body Weight</label><div>${u.weightKg || 0} kg</div></div>
                <div class="stat-card"><label>Total Burned</label><div>${finalCals} kcal</div></div>
                <div class="stat-card"><label>Active Time</label><div>${finalMins}m</div></div>
                <div class="stat-card"><label>Meals Logged</label><div>${meals.length}</div></div>
                <div class="stat-card"><label>Current Goal</label><div>${u.goal || 'N/A'}</div></div>
            </div>

            <div class="section-header"><h2>Workout History</h2></div>
            ${sessions.length > 0 ? `
            <table>
                <thead>
                    <tr><th>Date</th><th>Workout</th><th>Duration</th><th>Cals</th><th>Exercises</th><th>Difficulty</th></tr>
                </thead>
                <tbody>
                    ${sessions.map(s => `
                        <tr>
                            <td>${toJsDate(s.date).toLocaleDateString()}</td>
                            <td><b>${s.workoutName || 'Workout'}</b></td>
                            <td>${Math.floor(asNum(s.durationSeconds)/60)}m</td>
                            <td>${asNum(s.caloriesBurned)}</td>
                            <td>${asNum(s.exercisesDone)} / ${asNum(s.totalExercises)}</td>
                            <td><span class="badge">${s.difficulty || 'N/A'}</span></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
            ` : `<div class="empty-msg">No workout history found.</div>`}

            <div class="section-header"><h2>Performance Records</h2></div>
            ${records.length > 0 ? `
            <table>
                <thead>
                    <tr><th>Date</th><th>Exercise</th><th>Sets/Reps</th><th>Weight</th><th>ID</th></tr>
                </thead>
                <tbody>
                    ${records.map(r => `
                        <tr>
                            <td>${toJsDate(r.date).toLocaleDateString()}</td>
                            <td><b>${r.exerciseName || 'N/A'}</b></td>
                            <td>${r.reps || 'N/A'}</td>
                            <td>${asNum(r.weightKg)} kg</td>
                            <td><small style="color:var(--muted)">#${r.workoutId || '-'}</small></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
            ` : `<div class="empty-msg">No individual exercise performance records found.</div>`}

            <div class="section-header"><h2>Nutrition Logs</h2></div>
            ${meals.length > 0 ? `
            <table>
                <thead>
                    <tr><th>Date</th><th>Meal Name</th><th>Time</th><th>Cals</th><th>Prot</th><th>Carb</th><th>Fat</th></tr>
                </thead>
                <tbody>
                    ${meals.map(m => `
                        <tr>
                            <td>${toJsDate(m.date).toLocaleDateString()}</td>
                            <td><b>${m.name}</b></td>
                            <td>${m.mealTime || 'N/A'}</td>
                            <td>${asNum(m.calories)}</td>
                            <td>${asNum(m.protein)}g</td>
                            <td>${asNum(m.carbs)}g</td>
                            <td>${asNum(m.fats)}g</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
            ` : `<div class="empty-msg">No nutritional logs found.</div>`}

            <div class="footer">
                This report is automatically generated by the Nutriority Admin Dashboard. &copy; ${new Date().getFullYear()} Nutriority.
            </div>
        </body>
        </html>`;

        reportWindow.document.write(htmlContent);
        reportWindow.document.close();

    } catch (error) {
        console.error("Report Generation Failed:", error);
        alert("Report Error: " + error.message);
    }
}

/**
 * Professional HTML Summary Export for All Users
 */
export async function exportAllUsersSummary(users) {
    if (!users || !users.length) return alert("No user data loaded.");

    const reportWindow = window.open('', '_blank');
    if (!reportWindow) {
        alert("Popup Blocked! Please allow popups to view the summary report.");
        return;
    }

    // Initial Loading State
    reportWindow.document.write(`
        <div style="font-family: 'Plus Jakarta Sans', sans-serif; text-align: center; padding: 100px 50px;">
            <h2 style="color: #00A78B; font-weight: 800;">Nutriority Admin</h2>
            <p style="color: #64748b;">Generating Comprehensive User Summary Report...</p>
            <div class="loader"></div>
            <p style="font-size: 13px; color: #94a3b8;">Processing ${users.length} users. This may take a moment.</p>
            <style>
                .loader { border: 4px solid #f3f3f3; border-top: 4px solid #00A78B; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 30px auto; }
                @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
            </style>
        </div>
    `);

    try {
        const activeUsers = users.filter(u => u.status !== 'deleted');
        
        // Fetch subcollection data for each user in parallel to ensure "0" values are fixed
        const userStatsPromises = activeUsers.map(async (u) => {
            const [sessionSnap, mealSnap] = await Promise.all([
                getDocs(collection(db, "users", u.fid, "session_history")),
                getDocs(collection(db, "users", u.fid, "daily_meal_logs"))
            ]);

            let calcCals = 0;
            let calcMins = 0;
            sessionSnap.forEach(doc => {
                const s = doc.data();
                calcCals += asNum(s.caloriesBurned);
                calcMins += Math.floor(asNum(s.durationSeconds) / 60);
            });

            return {
                name: u.name || 'Unknown User',
                email: u.email || 'N/A',
                goal: u.goal || 'Not Set',
                cals: Math.max(asNum(u.totalCaloriesBurned), calcCals),
                mins: Math.max(asNum(u.totalWorkoutMinutes), calcMins),
                meals: mealSnap.size,
                progress: u.lastCompletedWorkoutDay || 0
            };
        });

        const results = await Promise.all(userStatsPromises);
        results.sort((a, b) => a.name.localeCompare(b.name));

        const htmlContent = `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>All Users Summary Report</title>
            <link rel="icon" type="image/png" href="assets/logo.png">
            <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
            <style>
                :root { --primary: #00A78B; --bg: #ffffff; --text: #1e293b; --muted: #64748b; --border: #e2e8f0; }
                body { font-family: 'Plus Jakarta Sans', sans-serif; color: var(--text); padding: 40px; background: #fff; }
                .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid var(--primary); padding-bottom: 20px; margin-bottom: 30px; }
                .header-brand { display: flex; align-items: center; gap: 15px; }
                .header-brand img { height: 50px; width: auto; }
                .header h1 { color: var(--primary); margin: 0; font-size: 26px; font-weight: 800; }
                .header p { margin: 5px 0 0 0; color: var(--muted); font-size: 14px; }
                
                table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                th { text-align: left; padding: 12px 15px; background: #f8fafc; color: var(--muted); font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid var(--border); }
                td { padding: 15px; border-bottom: 1px solid #f1f5f9; font-size: 13px; }
                tr:nth-child(even) { background: #fafbfc; }
                
                .val-bold { font-weight: 700; color: #0f172a; }
                .badge { padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; background: #f1f5f9; color: var(--muted); }
                
                .print-btn { background: var(--primary); color: white; padding: 10px 20px; border-radius: 8px; border: none; font-weight: 700; cursor: pointer; transition: 0.2s; }
                .print-btn:hover { background: #008f72; }
                
                @media print { .no-print { display: none !important; } body { padding: 0; } }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="header-brand">
                    <img src="assets/logo.png" alt="Logo" onerror="this.style.display='none'">
                    <div>
                        <h1>Nutriority User Summary</h1>
                        <p>Generated on ${new Date().toLocaleDateString()} at ${new Date().toLocaleTimeString()}</p>
                    </div>
                </div>
                <button class="print-btn no-print" onclick="window.print()">Print Report / Save PDF</button>
            </div>
            
            <table>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Goal</th>
                        <th style="text-align:center">Cals Burned</th>
                        <th style="text-align:center">Active Mins</th>
                        <th style="text-align:center">Meals Logged</th>
                        <th style="text-align:center">Current Day</th>
                    </tr>
                </thead>
                <tbody>
                    ${results.map(r => `
                        <tr>
                            <td><b>${r.name}</b><br><small style="color:var(--muted)">${r.email}</small></td>
                            <td><span class="badge">${r.goal}</span></td>
                            <td style="text-align:center" class="val-bold">${r.cals}</td>
                            <td style="text-align:center" class="val-bold">${r.mins}m</td>
                            <td style="text-align:center" class="val-bold">${r.meals}</td>
                            <td style="text-align:center">Day ${r.progress}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
            
            <div style="margin-top: 40px; text-align: center; color: var(--muted); font-size: 11px; border-top: 1px solid var(--border); padding-top: 20px;">
                &copy; ${new Date().getFullYear()} Nutriority Health Systems. This document contains confidential user data.
            </div>
        </body>
        </html>`;

        reportWindow.document.open();
        reportWindow.document.write(htmlContent);
        reportWindow.document.close();

    } catch (error) {
        console.error("Summary Export Failed:", error);
        reportWindow.document.write(`<div style="color:red; font-family:sans-serif; padding:50px;">Error: ${error.message}</div>`);
    }
}

window.downloadUserReport = downloadUserReport;
window.exportAllUsersSummary = () => exportAllUsersSummary(window.cacheSt);
