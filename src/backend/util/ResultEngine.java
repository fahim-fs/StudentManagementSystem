package backend.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ResultEngine — সব grading calculation এক জায়গায়।
 *
 * Marking scheme (total 300):
 *   CT       : best 3 of 4 × (300 × 20%) = best3 out of 60
 *   Attendance: 300 × 10% = 30 marks max, scaled by attendance %
 *              ≥80% → full 30 | every 5% drop → −3 marks | <60% → only 15 (5%)
 *   Term Final: faculty input, scaled to 210 (300 × 70%)
 *
 * Grading (out of 4.00):
 *   A+  ≥ 280/300  → 4.00
 *   A   ≥ 265      → 3.75
 *   A-  ≥ 250      → 3.50
 *   B+  ≥ 235      → 3.25
 *   B   ≥ 220      → 3.00
 *   B-  ≥ 205      → 2.75
 *   C+  ≥ 190      → 2.50
 *   C   ≥ 175      → 2.25
 *   D   ≥ 150      → 2.00
 *   F   <  150     → 0.00
 */
public class ResultEngine {

    // ── Attendance mark (out of 30) ───────────────────────────────────────────
    public static double calcAttendanceMark(double attendancePct) {
        if (attendancePct >= 80) return 30.0;
        if (attendancePct < 60)  return 15.0;   // 5% of 300

        // 60–79%: every 5% below 80 → lose 3 marks
        // 79% → 30, 74% → 27, 69% → 24, 64% → 21, 60% → 18 (মানে 5 steps × 3 = 15 কমে)
        // but minimum at <60 is 15, so at 60% = 30 - (4 steps × 3) = 18
        double stepsBelow80 = Math.floor((80 - attendancePct) / 5.0);
        double mark = 30.0 - (stepsBelow80 * 3.0);
        return Math.max(mark, 15.0);
    }

    // ── CT mark scaled to 60 (20% of 300) ────────────────────────────────────
    // best3 is already sum of best 3 CT marks (each out of 20), max = 60
    public static double calcCTMark(double best3Sum) {
        return Math.min(best3Sum, 60.0);
    }

    // ── Term Final scaled to 210 (70% of 300) ────────────────────────────────
    public static double calcTermFinalMark(double rawMarks, double fullMarks) {
        if (fullMarks <= 0) return 0;
        return (rawMarks / fullMarks) * 210.0;
    }

    // ── Total out of 300 ──────────────────────────────────────────────────────
    public static double calcTotal(double ctMark, double attendanceMark, double termFinalMark) {
        return ctMark + attendanceMark + termFinalMark;
    }

    // ── Letter grade ──────────────────────────────────────────────────────────
    public static String letterGrade(double total) {
        if (total >= 280) return "A+";
        if (total >= 265) return "A";
        if (total >= 250) return "A-";
        if (total >= 235) return "B+";
        if (total >= 220) return "B";
        if (total >= 205) return "B-";
        if (total >= 190) return "C+";
        if (total >= 175) return "C";
        if (total >= 150) return "D";
        return "F";
    }

    // ── Grade point (out of 4.00) ─────────────────────────────────────────────
    public static double gradePoint(double total) {
        if (total >= 280) return 4.00;
        if (total >= 265) return 3.75;
        if (total >= 250) return 3.50;
        if (total >= 235) return 3.25;
        if (total >= 220) return 3.00;
        if (total >= 205) return 2.75;
        if (total >= 190) return 2.50;
        if (total >= 175) return 2.25;
        if (total >= 150) return 2.00;
        return 0.00;
    }

    // ── Term GPA (credit-weighted average) ────────────────────────────────────
    public static double calcTermGPA(double[] gradePoints, double[] credits) {
        double totalWeighted = 0, totalCredit = 0;
        for (int i = 0; i < gradePoints.length; i++) {
            totalWeighted += gradePoints[i] * credits[i];
            totalCredit   += credits[i];
        }
        if (totalCredit == 0) return 0;
        return round2(totalWeighted / totalCredit);
    }

    // ── CGPA (average of all published term GPAs) ─────────────────────────────
    public static double calcCGPA(double[] termGPAs) {
        if (termGPAs.length == 0) return 0;
        double sum = 0;
        for (double g : termGPAs) sum += g;
        return round2(sum / termGPAs.length);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static double getBest3(double[] ctArr) {
        if (ctArr == null || ctArr.length == 0) return 0;
        List<Double> valid = new ArrayList<>();
        for (double m : ctArr) if (m >= 0) valid.add(m);
        valid.sort(Collections.reverseOrder());
        double sum = 0;
        for (int i = 0; i < Math.min(3, valid.size()); i++) sum += valid.get(i);
        return sum;
    }

    // ── Aliases ───────────────────────────────────────────────────────────────
    public static double calcGradePoint(double total)  { return gradePoint(total); }
    public static String calcLetterGrade(double total) { return letterGrade(total); }
}
