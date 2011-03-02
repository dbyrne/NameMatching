package byrne.mitre;

public class NameEntry {

         private String fullName;
         private String id;

         public NameEntry(String line) {
                 String[] vals = line.split("\\|");
                 if (vals[1].equals("FNU")) {
                         vals[1] = "";
                 }
                 id = vals[0];
                 fullName = " " + vals[1] + " " + vals[2] + " ";
                 fullName = fullName.replaceAll("-", " ");
                 fullName = fullName.replaceAll("'", "");
         }

         public String getFullName() {
                 return fullName;
         }

         public String getID() {
                 return id;
         }

 }
