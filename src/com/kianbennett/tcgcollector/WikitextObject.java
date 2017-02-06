package com.kianbennett.tcgcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WikitextObject {

    public class Table {
        public String name;
        public List<TableProperty> properties;

        public Table() {
            name = "";
            properties = new ArrayList<>();
        }
        public TableProperty getProperty(String key) {
            for (int i = 0; i < properties.size(); i++) {
                if (properties.get(i).key.equals(key)) {
                    return properties.get(i);
                }
            }
            return null;
        }
        public boolean hasProperty(String key) {
            return getProperty(key) != null;
        }
    }
    public class TableProperty {
        public String key, value;
        public String heading;
        public List<Table> tables;
        public List<ListProperty> lists;
        public List<Link> links;
        public TableProperty() {
            key = "";
            value = "";
            links = new ArrayList<>();
            tables = new ArrayList<>();
            lists = new ArrayList<>();
        }
        public Table getTable(String name) {
            for (int i = 0; i < tables.size(); i++) {
                if (tables.get(i).name.equals(name)) return tables.get(i);
            }
            return null;
        }
    }
    public class ListProperty extends TableProperty {
        public List<ListProperty> subLists;
        public int level;
        public ListProperty() {
            subLists = new ArrayList<>();
        }
    }
    public class Link {
        public String markupOrig = "";
        public String text;
        public String pageTitle;
        public void parse() { // Gets text and pageTitle from markupOrig
            markupOrig = "[[" + markupOrig + "]]";
            String s = markupOrig;
            s = s.replace("[[", "");
            s = s.replace("]]", "");
            String[] split = s.split(Pattern.quote("|"));
            if(split.length > 1) {
                pageTitle = split[0].trim();
                text = split[1].trim();
            } else {
                pageTitle = text = s;
            }
        }
    }

    public List<Table> tables;
    public List<TableProperty> properties;
    public List<ListProperty> lists;

    public WikitextObject(String wikiText) {
        parseText(wikiText);
    }

    private void parseText(String text) {
        tables = new ArrayList<>();
        properties = new ArrayList<>();
        lists = new ArrayList<>();
        TableProperty propertyCurrent = null;
        Table tableCurrent = null;
        Map<Integer, ListProperty> recentLists = new HashMap<Integer, ListProperty>();
        String headingCurrent = null;

        String[] lines = text.split(Pattern.quote("\n"));

        for(int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if(line.startsWith("{{")) {
                Table table = new Table();

                if(line.endsWith("}}")) { // Get all table information on that line and move to the next table
                    parseSingleLineTable(line, table);
                } else { // Take only the name from that line and keep working on that table for the next line
                    table.name = line.replace("{{", "");
                    tableCurrent = table;
                }

                if(propertyCurrent != null) {
                    propertyCurrent.tables.add(table);
                } else {
                    tables.add(table);
                }
            }
            if(line.startsWith("}}")) {
                tableCurrent = null;
                propertyCurrent = null;
            }

            if(line.startsWith("=") && line.endsWith("=")) {
                headingCurrent = line.replaceAll("=", "").trim();
            }

            if(line.startsWith("|") || line.startsWith("*")) {
                TableProperty property = null;
                if(line.startsWith("|")) {
                    property = new TableProperty();
                    propertyCurrent = property;
                    String[] split = line.split("=");
                    property.key = split[0].replace("|", "").trim();
                    if(split.length > 1) property.value = split[1].trim();
                    if(tableCurrent != null) {
                        tableCurrent.properties.add(property);
                    } else {
                        properties.add(property);
                    }
                }
                if(line.startsWith("*")) {
                    property = new ListProperty();

                    property.key = line.subSequence(0, line.lastIndexOf("*")).toString().trim();
                    property.value = line.replaceAll(Pattern.quote("*"), "");

                    int level = line.lastIndexOf("*");

                    if(level > 0 && recentLists.get(level - 1) != null) {
                        ((ListProperty) property).level = level;
                        recentLists.get(level - 1).subLists.add((ListProperty) property);
                    } else {
                        if(propertyCurrent == null) {
                            if(tableCurrent != null) {
                                tableCurrent.properties.add(property);
                            } else {
                                lists.add((ListProperty) property);
                            }
                        } else {
                            propertyCurrent.lists.add((ListProperty) property);
                        }
                    }

                    property.heading = headingCurrent;
                    recentLists.put(level, (ListProperty) property);
                }

                parsePropertyValue(property);
            }

            if(line.startsWith("'''")) { // Special case scenario - some sets have their table info in this weird format
                TableProperty prop1 = new TableProperty(), prop2 = new TableProperty(), prop3 = new TableProperty();

                String[] split = line.split(Pattern.quote("("));
                prop1.value = split[0].replace("'''", "").trim();
                if(split.length > 1) {
                    String[] minusSplit = split[1].split(" - ");
                    prop2.value = minusSplit[0].replace(")", "").trim();
                    if(minusSplit.length > 1) {
                        prop3.value = minusSplit[1].replace(")", "").trim();
                    }
                }

                Table table = new Table();

                if(!prop1.value.equals("")) {
                    parsePropertyValue(prop1);
                    prop1.key = prop1.value;
                    prop1.value = "";
                    table.properties.add(prop1);
                }
                if(!prop2.value.equals("")) {
                    parsePropertyValue(prop2);
                    prop2.key = prop2.value;
                    prop2.value = "";
                    table.properties.add(prop2);
                }
                if(!prop3.value.equals("")) {
                    parsePropertyValue(prop3);
                    prop3.key = prop3.value;
                    prop3.value = "";
                    if(prop3.key.equals("C")) prop3.key = "Common";
                    table.properties.add(prop3);
                }

                if(table.properties.size() > 0) {
                    if(propertyCurrent != null) {
                        propertyCurrent.tables.add(table);
                    } else {
                        tables.add(table);
                    }
                }
            }
        }
    }

    // Returns a table made from a string starting with "{{" and ending with "}}" on a single line
    private void parseSingleLineTable(String s, Table table) {
        s = s.replace("{{", "");
        s = s.replace("}}", "");
        String[] split = s.split(Pattern.quote("|"));
        table.name = split[0];
        for(int i = 0; i < split.length; i++) {
            if(i > 0) {
                TableProperty prop = new TableProperty();
                prop.key = split[i].trim();
                table.properties.add(prop);
            }
        }
    }

    private void parsePropertyValue(TableProperty property) {
        if(property.value.contains("{{") || property.value.contains("[[")) {
            boolean readingTableText = false, readingLinkText = false;
            String tableStringCurrent = "", linkStringCurrent = "";
            for(int c = 0; c < property.value.length(); c++) {
                Character propChar = property.value.charAt(c);
                Character propCharNext = null;
                if(c < property.value.length() - 1) propCharNext = property.value.charAt(c + 1);
                if (propChar == "{".charAt(0) && propCharNext == "{".charAt(0)) {
                    readingTableText = true;
                    c++;
                    continue;
                }
                if (propChar == "}".charAt(0) && propCharNext == "}".charAt(0)) {
                    readingTableText = false;
                    Table table = new Table();
                    parseSingleLineTable(tableStringCurrent, table);
                    tableStringCurrent = "";
                    property.tables.add(table);
                    c++;
                    continue;
                }
                if (propChar == "[".charAt(0) && propCharNext == "[".charAt(0)) {
                    readingLinkText = true;
                    c++;
                    continue;
                }
                if (propChar == "]".charAt(0) && propCharNext == "]".charAt(0)) {
                    readingLinkText = false;
                    Link link = new Link();
                    link.markupOrig = linkStringCurrent;
                    link.parse();
                    property.links.add(link);
                    linkStringCurrent = "";
                    c++;
                    continue;
                }
                if(readingTableText) tableStringCurrent += propChar;
                if(readingLinkText) linkStringCurrent += propChar;
            }
            for(int l = 0; l < property.links.size(); l++) {
                property.value = property.value.replace(property.links.get(l).markupOrig, property.links.get(l).text);
            }
            property.value = property.value.replace("''", "");
        }
    }

    public Table getTable(String name) {
        for (int i = 0; i < tables.size(); i++) {
            if (tables.get(i).name.equals(name)) {
                return tables.get(i);
            }
        }
        return null;
    }
}