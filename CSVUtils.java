import java.io.*;
import java.nio.file.*;
import java.util.*;
public class CSVUtils {
    public static void saveStudents(Path file, List<Student> students) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.write("name,subject,grades,weight\n");
            for (Student s : students) {
                for (String line : s.toCsvLines()) {
                    bw.write(line + "\n");
                }
            }
        }
    }
 List<Student> loadStudents(Path file) throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        Map<String, Student> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String header = br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 3) continue;
                String name = parts.get(0).trim();
                String subject = parts.get(1).trim();
                String gradesPart = parts.get(2).trim();
                double weight = 1.0;
                if (parts.size() >= 4 && !parts.get(3).isEmpty()) {
                    try { weight = Double.parseDouble(parts.get(3)); } catch (NumberFormatException ignored) {}
                }
                Student s = map.computeIfAbsent(name.toLowerCase(), k -> new Student(name));
                s.setSubjectWeight(subject, weight);
                if (!gradesPart.isEmpty()) {
                    String[] gs = gradesPart.split(";");
                    for (String g : gs) {
                        try { int gi = Integer.parseInt(g.trim()); s.addGrade(subject, gi); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return new ArrayList<>(map.values());
    }
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') { cur.append('\"'); i++; }
                    else { inQuotes = false; }
                } else cur.append(c);
            } else {
                if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else if (c == '\"') { inQuotes = true; }
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
