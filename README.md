# SInC: **S**emantic **In**ductive **C**ompressor
SInC is a semantic inductive compressor on relational data. It splits DBs into two parts where one can be semantically inferred by the other, thus the inferable part is reduced for compression. The compression is realized by iteratively mining for SQL queries until there are no more records that can be effectively covered by any other pattern.

## 1. Prerequisites

SInC is implemented in Java and require version 11+. All of its dependencies are publicly accessible via Maven.

Input data format is `.tsv` files. Each line of the file is the following format:

```
<relation>\t<arg1>\t<arg2>\t...<argn>
```

For example:

```
President	USA	Trump
Family	ginny	harry	albus
```

## 2. Use SInC Implementation

The basic class for compression is the abstract class `SInC`. To compress relational data, simply create a SInC implementation object and invoke `run()` on it. For exmaple:

```java
SincConfig config = new SincConfig(1,false,false,5,true,eval_type,0.05,0.25,false,-1.0,false,false);
SInC sinc = new SincWithRecalculateCache(config,"elti.tsv",null,null);
sinc.run();
List<Rule> hypothesis = sinc.getHypothesis();  // H
Set<Predicate> necessary = sinc.getStartSet();  // N
Set<Predicate> counter_examples = sinc.getCounterExamples();  // C
Set<String> delta_constants = sinc.getSupplementaryConstants();  // ∆Σ
```

Arguments in `SincConfig` are for debugging, experiments and possible extensions. Currently, the following arguments will take effect:

- `beamWidth`
- `evalMetric`
- `minFactCoverage`
- `minConstantCoverage`

Current implementation of `SInC` is `SincWithTabuPruning`, other implementations are for experiments. Arguments to initialize a SInC object include:

- `config`: A `SincConfig` object
- `kbPath`: The path to the input data file
- `dumpPath`: The path to compressed data file. If null, output will go to the terminal
- `logPath`: The path to a log file. If null, output will go to the terminal

## 3. Use SInC Jar

The `Main` class in package `sinc` provides a `main()` method that encloses all features. To use this entry, you can package the whole project with dependencies and run with: `java -jar sinc.jar [Options]`. The following displays the usage:

```
usage: java -jar sinc.jar [-b <b>] [-c <cc>] [-d <path> | -D <name>]  [-e
       <name>] [-f <fc>] [-h] [-l <path>] [-m <name>] [-r <path>] [-v]
 -b,--beam-width <b>        Bean search width (Default 3)
 -c,--const-coverage <cc>   Set constant coverage threshold (Default 0.25)
 -d,--data-path <path>      Path to a data file
 -D,--dataset <name>        Select in the prepared datasets. Available
                            options are: E(Elti), D(Dunur), S(Student
                            Loan), DBf(DBpedia.factbook),
                            DBl(DBpedia.lobidorg), WKc(WebKB.Cornell),
                            WKt(WebKB.Texas), WKw(WebKB.Washington),
                            WKi(WebKB.Wisconsin), N(NELL), U(UMLS),
                            WN(WN18), FB(FB15K), YS(YagoSample),
                            Fs(Family.simple), Fm(Family.medium),
                            OS(Online Sales), RR(Restaurant Ranking),
                            Test(Test)
 -e,--eval-metric <name>    Select in the evaluation metrics (Default δ).
                            Available options are: τ(Compression Rate),
                            δ(Compression Capacity), h(Information Gain),
                            H(Cumulated Information)
 -f,--fact-coverage <fc>    Set fact coverage threshold (Default 0.05)
 -h,--help                  Display this help
 -l,--log-path <path>       Path to where the log is dumped (StdOut if not
                            appointed)
 -m,--model <name>          Select in the models (Default T). Available
                            options are: C(SInC with compact cache),
                            M(SInC with materialized cache), T(Model C
                            with tabu pruning), Y(Model T focus on
                            symmetric relations (for experiments only))
 -r,--result-path <path>    Path to where the result is dumped (StdOut if
                            not appointed)
 -v,--validate              Validate result after compression
```

