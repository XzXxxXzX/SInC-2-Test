package sinc;

import org.apache.commons.cli.*;
import sinc.common.Eval;
import sinc.common.Model;
import sinc.common.Dataset;
import sinc.common.SincException;

public class Main {

    public static final int DEFAULT_BEAM_WIDTH = 3;
    public static final double DEFAULT_FACT_COVERAGE = 0.05;
    public static final double DEFAULT_CONSTANT_COVERAGE = 0.25;
    public static final double DEFAULT_COLUMN_SIMILARITY = 0.1;
    public static final double DEFAULT_STOP_COMPRESSION_RATE = 0.9;
    public static final Eval.EvalMetric DEFAULT_EVAL_METRIC = Eval.EvalMetric.CompressionCapacity;
    public static final Model DEFAULT_MODEL = Model.TABU;

    private static final String SHORT_OPT_BEAM_WIDTH = "b";
    private static final String SHORT_OPT_VALIDATE = "v";
    private static final String SHORT_OPT_FACT_COVERAGE = "f";
    private static final String SHORT_OPT_CONSTANT_COVERAGE = "c";
    private static final String SHORT_OPT_COLUMN_SIMILARITY = "s";
    private static final String SHORT_OPT_STOP_COMPRESSION_RATE = "p";
    private static final String SHORT_OPT_RESULT_PATH = "r";
    private static final String SHORT_OPT_LOG_PATH = "l";
    private static final String SHORT_OPT_HELP = "h";
    private static final String SHORT_OPT_DATASET = "D";
    private static final String SHORT_OPT_DATA_PATH = "d";
    private static final String SHORT_OPT_EVAL_METRIC = "e";
    private static final String SHORT_OPT_MODEL = "m";
    private static final String LONG_OPT_BEAM_WIDTH = "beam-width";
    private static final String LONG_OPT_VALIDATE = "validate";
    private static final String LONG_OPT_FACT_COVERAGE = "fact-coverage";
    private static final String LONG_OPT_CONSTANT_COVERAGE = "const-coverage";
    private static final String LONG_OPT_COLUMN_SIMILARITY = "column-similarity";
    private static final String LONG_OPT_STOP_COMPRESSION_RATE = "stop-comp-rate";
    private static final String LONG_OPT_RESULT_PATH = "result-path";
    private static final String LONG_OPT_LOG_PATH = "log-path";
    private static final String LONG_OPT_HELP = "help";
    private static final String LONG_OPT_DATA_PATH = "data-path";
    private static final String LONG_OPT_DATASET = "dataset";
    private static final String LONG_OPT_EVAL_METRIC = "eval-metric";
    private static final String LONG_OPT_MODEL = "model";

    private static final Option OPTION_BEAM_WIDTH = Option.builder(SHORT_OPT_BEAM_WIDTH).longOpt(LONG_OPT_BEAM_WIDTH)
            .desc(String.format("Bean search width (Default %d)", DEFAULT_BEAM_WIDTH)).argName("b").hasArg().type(Integer.class).build();
    private static final Option OPTION_VALIDATE = Option.builder(SHORT_OPT_VALIDATE).longOpt(LONG_OPT_VALIDATE)
            .desc("Validate result after compression").build();
    private static final Option OPTION_FACT_COVERAGE = Option.builder(SHORT_OPT_FACT_COVERAGE).longOpt(LONG_OPT_FACT_COVERAGE)
            .desc(String.format("Set fact coverage threshold (Default %f)", DEFAULT_FACT_COVERAGE)).argName("fc").hasArg().type(Double.class).build();
    private static final Option OPTION_CONSTANT_COVERAGE = Option.builder(SHORT_OPT_CONSTANT_COVERAGE).longOpt(LONG_OPT_CONSTANT_COVERAGE)
            .desc(String.format("Set constant coverage threshold (Default %f)", DEFAULT_CONSTANT_COVERAGE)).argName("cc").hasArg().type(Double.class).build();
    private static final Option OPTION_COLUMN_SIMILARITY = Option.builder(SHORT_OPT_COLUMN_SIMILARITY).longOpt(LONG_OPT_COLUMN_SIMILARITY)
            .desc(String.format("Set column similarity threshold (Default %f)", DEFAULT_COLUMN_SIMILARITY)).argName("cs").hasArg().type(Double.class).build();
    private static final Option OPTION_STOP_COMPRESSION_RATE = Option.builder(SHORT_OPT_STOP_COMPRESSION_RATE).longOpt(LONG_OPT_STOP_COMPRESSION_RATE)
            .desc(String.format("Set stopping compression rate (Default %f)", DEFAULT_STOP_COMPRESSION_RATE)).argName("scr").hasArg().type(Double.class).build();
    private static final Option OPTION_RESULT_PATH = Option.builder(SHORT_OPT_RESULT_PATH).longOpt(LONG_OPT_RESULT_PATH)
            .desc("Path to where the result is dumped (StdOut if not appointed)").argName("path").hasArg().type(String.class).build();
    private static final Option OPTION_LOG_PATH = Option.builder(SHORT_OPT_LOG_PATH).longOpt(LONG_OPT_LOG_PATH)
            .desc("Path to where the log is dumped (StdOut if not appointed)").argName("path").hasArg().type(String.class).build();
    private static final Option OPTION_HELP = Option.builder(SHORT_OPT_HELP).longOpt(LONG_OPT_HELP)
            .desc("Display this help").build();
    private static final Option OPTION_DATA_PATH = Option.builder(SHORT_OPT_DATA_PATH).longOpt(LONG_OPT_DATA_PATH)
            .desc("Path to a data file").argName("path").hasArg().type(String.class).build();
    private static final Option OPTION_DATASET = Option.builder(SHORT_OPT_DATASET).longOpt(LONG_OPT_DATASET)
            .argName("name").hasArg().type(String.class).build();
    private static final OptionGroup OPT_GRP_DATA = new OptionGroup();
    private static final Option OPTION_EVAL_METRIC = Option.builder(SHORT_OPT_EVAL_METRIC).longOpt(LONG_OPT_EVAL_METRIC)
            .argName("name").hasArg().type(String.class).build();
    private static final Option OPTION_MODEL = Option.builder(SHORT_OPT_MODEL).longOpt(LONG_OPT_MODEL)
            .argName("name").hasArg().type(String.class).build();

    static {
        OPT_GRP_DATA.addOption(OPTION_DATA_PATH).addOption(OPTION_DATASET);

        /* List Available Datasets */
        Dataset[] datasets = Dataset.values();
        StringBuilder dataset_desc_builder = new StringBuilder("Select in the prepared datasets. Available options are: ")
                .append(datasets[0].getShortName()).append('(').append(datasets[0].getName()).append(')');
        for (int i = 1; i < datasets.length; i++) {
            Dataset dataset = datasets[i];
            dataset_desc_builder.append(", ").append(dataset.getShortName()).append('(').append(dataset.getName()).append(')');
        }
        OPTION_DATASET.setDescription(dataset_desc_builder.toString());

        /* List Available Eval Metrics */
        Eval.EvalMetric[] metrics = Eval.EvalMetric.values();
        StringBuilder eval_metric_desc_builder = new StringBuilder("Select in the evaluation metrics (Default ")
                .append(DEFAULT_EVAL_METRIC.getName()).append("). Available options are: ")
                .append(metrics[0].getName()).append('(').append(metrics[0].getDescription()).append(')');
        for (int i = 1; i < metrics.length; i++) {
            Eval.EvalMetric metric = metrics[i];
            eval_metric_desc_builder.append(", ").append(metric.getName()).append('(').append(metric.getDescription()).append(')');
        }
        OPTION_EVAL_METRIC.setDescription(eval_metric_desc_builder.toString());

        /* List Available Models */
        Model[] models = Model.values();
        StringBuilder model_desc_builder = new StringBuilder("Select in the models (Default ")
                .append(DEFAULT_MODEL.getName()).append("). Available options are: ")
                .append(models[0].getName()).append('(').append(models[0].getDescription()).append(')');
        for (int i = 1; i < models.length; i++) {
            Model model = models[i];
            model_desc_builder.append(", ").append(model.getName()).append('(').append(model.getDescription()).append(')');
        }
        OPTION_MODEL.setDescription(model_desc_builder.toString());
    }

    public static void main(String[] args) throws Exception{
        Options options = buildOptions();
        SInC sinc = parseArgs(options, args);
        if (null != sinc) {
            sinc.run();
        }
    }

    protected static SInC parseArgs(Options options, String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        /* Help */
        if (cmd.hasOption(SHORT_OPT_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar sinc.jar", options, true);
            return null;
        }

        /* Assign Beam */
        int beam = DEFAULT_BEAM_WIDTH;
        if (cmd.hasOption(SHORT_OPT_BEAM_WIDTH)) {
            String value = cmd.getOptionValue(SHORT_OPT_BEAM_WIDTH);
            if (null != value) {
                beam = Integer.parseInt(value);
                System.out.println("Beam width set to: " + beam);
            }
        }

        /* Determine Validation */
        boolean validation = cmd.hasOption(SHORT_OPT_VALIDATE);

        /* Select Eval Metric (e) */
        Eval.EvalMetric metric = DEFAULT_EVAL_METRIC;
        if (cmd.hasOption(SHORT_OPT_EVAL_METRIC)) {
            String value = cmd.getOptionValue(SHORT_OPT_EVAL_METRIC);
            if (null != value) {
                metric = Eval.EvalMetric.getByName(value);
                if (null == metric) {
                    throw new SincException("Unknown evaluation metric: " + value);
                }
                System.out.println("Evaluation metric set to: " + metric.getName());
            }
        }

        /* Assign FC Threshold (f) */
        double fc = DEFAULT_FACT_COVERAGE;
        if (cmd.hasOption(SHORT_OPT_FACT_COVERAGE)) {
            String value = cmd.getOptionValue(SHORT_OPT_FACT_COVERAGE);
            if (null != value) {
                fc = Double.parseDouble(value);
                System.out.println("Fact coverage set to: " + fc);
            }
        }

        /* Assign CC Threshold (c) */
        double cc = DEFAULT_CONSTANT_COVERAGE;
        if (cmd.hasOption(SHORT_OPT_CONSTANT_COVERAGE)) {
            String value = cmd.getOptionValue(SHORT_OPT_CONSTANT_COVERAGE);
            if (null != value) {
                cc = Double.parseDouble(value);
                System.out.println("Constant coverage set to: " + cc);
            }
        }

        /* Assign CS Threshold (s) */
        double cs = DEFAULT_COLUMN_SIMILARITY;
        if (cmd.hasOption(OPTION_COLUMN_SIMILARITY)) {
            String value = cmd.getOptionValue(OPTION_COLUMN_SIMILARITY);
            if (null != value) {
                cs = Double.parseDouble(value);
                System.out.println("Column similarity set to: " + cs);
            }
        }

        /* Assign CS Threshold (s) */
        double scr = DEFAULT_STOP_COMPRESSION_RATE;
        if (cmd.hasOption(OPTION_STOP_COMPRESSION_RATE)) {
            String value = cmd.getOptionValue(OPTION_STOP_COMPRESSION_RATE);
            if (null != value) {
                scr = Double.parseDouble(value);
                System.out.println("Stopping compression rate set to: " + scr);
            }
        }

        /* Select Dataset */
        String data_path = null;
        if (cmd.hasOption(SHORT_OPT_DATA_PATH)) {
            data_path = cmd.getOptionValue(SHORT_OPT_DATA_PATH);
        } else if (cmd.hasOption(SHORT_OPT_DATASET)) {
            String value = cmd.getOptionValue(SHORT_OPT_DATASET);
            Dataset dataset = Dataset.getByShortName(value);
            if (null == dataset) {
                throw new SincException("Unknown dataset: " + value);
            }
            data_path = dataset.getPath();
        }
        if (null != data_path) {
            System.out.println("Data path set to: " + data_path);
        }

        /* Assign Dump Path */
        String result_path = null;
        if (cmd.hasOption(OPTION_RESULT_PATH)) {
            result_path = cmd.getOptionValue(OPTION_RESULT_PATH);
            if (null != result_path) {
                System.out.println("Result path set to: " + result_path);
            }
        }

        /* Assign Log Path */
        String log_path = null;
        if (cmd.hasOption(OPTION_LOG_PATH)) {
            log_path = cmd.getOptionValue(OPTION_LOG_PATH);
            if (null != log_path) {
                System.out.println("Log path set to: " + log_path);
            }
        }

        /* Create Model */
        Model model = DEFAULT_MODEL;
        if (cmd.hasOption(OPTION_MODEL)) {
            String value = cmd.getOptionValue(OPTION_MODEL);
            model = Model.getModelByName(value);
            if (null == model) {
                throw new SincException("Unknown model: " + value);
            }
            System.out.println("Model set to: " + model.getName());
        }

        SincConfig config = new SincConfig(
                1, validation, false, beam, false, metric, fc, cc, cs, scr, true,
                -1.0, false, false
        );
        return Model.getModel(model.getName(), config, data_path, result_path, log_path);
    }

    protected static Options buildOptions() {
        Options options = new Options();

        /* Model Selection (m) */
        options.addOption(OPTION_MODEL);

        /* Beam Assignment (b) */
        options.addOption(OPTION_BEAM_WIDTH);

        /* Validate (v) */
        options.addOption(OPTION_VALIDATE);

        /* Eval Metric Selection (e) */
        options.addOption(OPTION_EVAL_METRIC);

        /* FC Threshold Assignment (f) */
        options.addOption(OPTION_FACT_COVERAGE);

        /* CC Threshold Assignment (c) */
        options.addOption(OPTION_CONSTANT_COVERAGE);

        /* CS Threshold Assignment (s) */
        options.addOption(OPTION_COLUMN_SIMILARITY);

        /* CS Threshold Assignment (p) */
        options.addOption(OPTION_STOP_COMPRESSION_RATE);

        /* Dataset Selection (d, D) */
        options.addOptionGroup(OPT_GRP_DATA);

        /* Dump Path (r) */
        options.addOption(OPTION_RESULT_PATH);

        /* Log Path (l) */
        options.addOption(OPTION_LOG_PATH);

        /* Help */
        options.addOption(OPTION_HELP);

        return options;
    }
}
