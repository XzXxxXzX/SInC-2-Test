package sinc.common;

import java.util.HashMap;
import java.util.Map;

public enum Dataset {
    ELTI(Dir.DIR + "/elti.tsv", "Elti", "E"),
    DUNUR(Dir.DIR + "/dunur.tsv", "Dunur", "D"),
    STUDENT_LOAN(Dir.DIR + "/student_loan.tsv", "Student Loan", "S"),
    DBPEDIA_FACTBOOK(Dir.DIR + "/dbpedia_factbook.tsv", "DBpedia.factbook", "DBf"),
    DBPEDIA_LOBIDORG(Dir.DIR + "/dbpedia_lobidorg.tsv", "DBpedia.lobidorg", "DBl"),
    WEBKB_CORNELL(Dir.DIR + "/webkb.cornell.tsv", "WebKB.Cornell", "WKc"),
    WEBKB_TEXAS(Dir.DIR + "/webkb.texas.tsv", "WebKB.Texas", "WKt"),
    WEBKB_WASHINGTON(Dir.DIR + "/webkb.washington.tsv", "WebKB.Washington", "WKw"),
    WEBKB_WISCONSIN(Dir.DIR + "/webkb.wisconsin.tsv", "WebKB.Wisconsin", "WKi"),
    NELL(Dir.DIR + "/nell.tsv", "NELL", "N"),
    UMLS(Dir.DIR + "/UMLS.tsv", "UMLS", "U"),
    WN18(Dir.DIR + "/WN18.tsv", "WN18", "WN"),
    FB15K(Dir.DIR + "/FB15K.tsv", "FB15K", "FB"),
    YAGO_SAMPLE(Dir.DIR + "/Yago_sample.tsv", "YagoSample", "YS"),
    FAMILY_SIMPLE(Dir.DIR + "/family_simple.tsv", "Family.simple", "Fs"),
    FAMILY_MEDIUM(Dir.DIR + "/family_medium.tsv", "Family.medium", "Fm"),
    ONLINE_SALES(Dir.DIR + "/online_sales.tsv", "Online Sales", "OS"),
    RESTAURANT_RANKING(Dir.DIR + "/restaurant_ranking.tsv", "Restaurant Ranking", "RR"),
    CODEX(Dir.DIR + "/codex.tsv", "CoDeX", "CX"),
    Test(Dir.DIR + "/test.tsv", "Test", "Test")
    ;

    public static class Dir {
        public static final String DIR = "datasets";
    }
    
    private final String path;
    private final String name;
    private final String shortName;

    private static final Map<String, Dataset> shortName2ValMap = new HashMap<>();
    static {
        for (Dataset dataset: Dataset.values()) {
            shortName2ValMap.put(dataset.shortName, dataset);
        }
    }

    Dataset(String path, String name, String shortName) {
        this.path = path;
        this.name = name;
        this.shortName = shortName;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    static public Dataset getByShortName(String shortName) {
        return shortName2ValMap.get(shortName);
    }
}
