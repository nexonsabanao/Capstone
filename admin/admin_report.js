import { db } from "./firebase_init.js";
import { collection, getDocs, doc, getDoc, query, limit } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";

/**
 * Helper to safely get a number from Firestore data (handles strings or missing values)
 */
const asNum = (val) => {
    if (typeof val === 'number') return val;
    if (typeof val === 'string') return parseFloat(val) || 0;
    return 0;
};

/**
 * Generates and downloads a comprehensive CSV report for a single user.
 */
export async function downloadUserReport(uid, name) {
    try {
        console.log(`[REPORT] Starting fetch for: ${name} (${uid})`);
        
        // Fetch data without orderBy first to avoid index-related timeouts/errors
        const [sessionSnap, mealSnap, recordSnap, userDoc] = await Promise.all([
            getDocs(collection(db, "users", uid, "session_history")),
            getDocs(collection(db, "users", uid, "daily_meal_logs")),
            getDocs(collection(db, "users", uid, "exercise_records")),
            getDoc(doc(db, "users", uid))
        ]);

        console.log(`[REPORT] Fetched: ${sessionSnap.size} sessions, ${mealSnap.size} meals, ${recordSnap.size} records`);

        const u = userDoc.data() || {};
        
        // --- Aggregation Logic (Fallback for 0 stats) ---
        let calcCals = 0;
        let calcMins = 0;
        let calcWorkouts = 0;

        const sessions = [];
        sessionSnap.forEach(doc => {
            const d = doc.data();
            const cals = asNum(d.caloriesBurned);
            const dur = asNum(d.durationSeconds);
            calcCals += cals;
            calcMins += Math.floor(dur / 60);
            calcWorkouts++;
            sessions.push({ ...d, date: d.date || 0 });
        });

        // Sort manually in JS to avoid index requirements
        sessions.sort((a, b) => (b.date || 0) - (a.date || 0));

        const finalCals = asNum(u.totalCaloriesBurned) || calcCals;
        const finalMins = asNum(u.totalWorkoutMinutes) || calcMins;
        const finalWorkouts = asNum(u.totalWorkoutsCompleted) || calcWorkouts;

        let csvContent = "data:text/csv;charset=utf-8,";

        // SECTION 1: User Profile
        csvContent += "--- USER PROFILE ---\nField,Value\n";
        csvContent += `Name,${(u.name || 'N/A').replace(/,/g, '')}\n`;
        csvContent += `Email,${(u.email || 'N/A').replace(/,/g, '')}\n`;
        csvContent += `Goal,${(u.goal || 'N/A').replace(/,/g, '')}\n`;
        csvContent += `Weight,${u.weightKg || 0} kg\n`;
        csvContent += `Height,${u.heightCm || 0} cm\n`;
        csvContent += `Total Calories Burned,${finalCals}\n`;
        csvContent += `Total Workout Minutes,${finalMins}\n`;
        csvContent += `Total Workouts Completed,${finalWorkouts}\n\n`;

        // SECTION 2: Workout Sessions
        csvContent += "--- WORKOUT HISTORY ---\n";
        csvContent += "Date,Workout Name,Duration (m),Calories,Done,Total,Difficulty\n";
        sessions.forEach(d => {
            const dateStr = new Date(d.date).toLocaleDateString();
            csvContent += `${dateStr},"${(d.workoutName || 'Workout').replace(/"/g, '""')}",${Math.floor(asNum(d.durationSeconds)/60)},${asNum(d.caloriesBurned)},${asNum(d.exercisesDone)},${asNum(d.totalExercises)},${d.difficulty || 'N/A'}\n`;
        });
        csvContent += "\n";

        // SECTION 3: Meal Logs
        const meals = [];
        mealSnap.forEach(doc => meals.push({ ...doc.data(), id: doc.id }));
        meals.sort((a, b) => (b.date || 0) - (a.date || 0));

        csvContent += "--- NUTRITION LOGS ---\n";
        csvContent += "Date,Meal Name,Time,Calories,Protein,Carbs,Fats,Type\n";
        meals.forEach(d => {
            const dateStr = new Date(d.date).toLocaleDateString();
            const type = d.mealId === "-1" ? "Manual" : "Library";
            csvContent += `${dateStr},"${(d.name || 'Meal').replace(/"/g, '""')}",${d.mealTime || 'N/A'},${asNum(d.calories)},${asNum(d.protein)},${asNum(d.carbs)},${asNum(d.fats)},${type}\n`;
        });
        csvContent += "\n";

        // SECTION 4: Exercise Records
        csvContent += "--- EXERCISE PERFORMANCE RECORDS ---\n";
        csvContent += "Date,Exercise Name,Workout ID,Reps,Weight (kg)\n";
        if (recordSnap.empty) {
            csvContent += "No performance records found.,,,,\n";
        } else {
            const records = [];
            recordSnap.forEach(doc => records.push(doc.data()));
            records.sort((a, b) => {
                const da = a.date?.toDate ? a.date.toDate().getTime() : (a.date || 0);
                const db = b.date?.toDate ? b.date.toDate().getTime() : (b.date || 0);
                return db - da;
            });
            records.forEach(d => {
                const dateVal = d.date?.toDate ? d.date.toDate() : new Date(d.date);
                csvContent += `${dateVal.toLocaleDateString()},"${(d.exerciseName || 'N/A').replace(/"/g, '""')}",${d.workoutId || 'N/A'},"${(d.reps || '').replace(/"/g, '""')}",${asNum(d.weightKg)}\n`;
            });
        }

        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", `Nutriority_Report_${(name || 'User').replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
    } catch (error) {
        console.error("[REPORT ERROR]", error);
        alert("Failed to generate report. Error: " + error.message);
    }
}

/**
 * Optimized Summary Export: sequential fetching to avoid timeouts
 */
export async function exportAllUsersSummary(users) {
    if (!users || !users.length) return alert("No user data loaded.");

    if (!confirm(`Generate summary for ${users.length} users? This may take a few moments.`)) return;
    
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += "Name,Email,Goal,Activity Level,Progress (Day),Calories Burned,Workout Minutes,Workouts Completed\n";
    
    // Process in small chunks to prevent browser freeze and timeouts
    for (let i = 0; i < users.length; i++) {
        const u = users[i];
        if (u.status === 'deleted') continue;

        let totalCals = asNum(u.totalCaloriesBurned);
        let totalMins = asNum(u.totalWorkoutMinutes);
        let totalWorkouts = asNum(u.totalWorkoutsCompleted);

        // Only fetch sub-collection if totals are 0
        if (totalCals === 0) {
            try {
                const sessions = await getDocs(collection(db, "users", u.fid, "session_history"));
                sessions.forEach(sDoc => {
                    const sd = sDoc.data();
                    totalCals += asNum(sd.caloriesBurned);
                    totalMins += Math.floor(asNum(sd.durationSeconds) / 60);
                    totalWorkouts++;
                });
            } catch (e) {
                console.warn(`Could not fetch sessions for ${u.name}`);
            }
        }

        csvContent += `"${(u.name || 'User').replace(/"/g, '""')}",${u.email || 'N/A'},"${(u.goal || 'N/A').replace(/"/g, '""')}","${(u.activityLevel || 'N/A').replace(/"/g, '""')}",${u.lastCompletedWorkoutDay || 0},${totalCals},${totalMins},${totalWorkouts}\n`;
    }

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Nutriority_All_Users_Summary_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

window.downloadUserReport = downloadUserReport;
window.exportAllUsersSummary = () => exportAllUsersSummary(window.cacheSt);
