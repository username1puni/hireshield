# Model experiments

This page records what was tried to improve the scam detector, with the real numbers. The short version:
the served model did not change. The models that scored higher on job ads did much worse on real
SMS/WhatsApp job scams, so they were not shipped. What did change is how the system is tested, and a few
rules that now catch more SMS-style scams without adding false alarms.

## Setup

- **Main data**: EMSCAD, 17,880 job ads, 866 fake. 2,163 duplicates removed first (15,717 left, 708 fake).
- **Main test set**: stratified 20%, 3,144 ads (142 fake), put aside before anything else and only used
  for the final numbers. Everything below was decided with 5-fold cross validation on the other 80%,
  including the decision threshold of each model.
- **Second test set**: 430 real job-scam text messages from the IMC 2025 smishing dataset (CC BY 4.0).
  These are scam-only, so only recall (how many were caught) can be measured. They were never used for
  training. The dataset hides personal data with tags like `<URL>` and `<PHONE_NUMBER>`; these are
  replaced with something the text normalizer turns into the same token a real link or phone number gives,
  so the model sees what it would see in the app. Tags like `<NAMED_ENTITY>` are removed.

More training data was looked for first. Every public "fake job postings" dataset found online was either
a copy of EMSCAD (95-100% overlap) or generated text, and training on those would only inflate the
numbers. The one real new source, the IMC 2025 smishing data, has scam rows only, so it is used as a
test set rather than for training.

## Model comparison

All models use the same features: TF-IDF over words and word pairs (75,000 terms, sublinear term
frequency, L2 normalised). Class weights balance the 4.5% fake class.

| Model | CV F1 | Test precision | Test recall | Test F1 | Fake ads caught | False alarms (of 3,002) | SMS scams caught |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Logistic Regression (lr 0.5, 10 epochs) | 0.796 | 85.3% | 77.5% | 0.812 | 110/142 | 19 | 299/430 (69.5%) |
| Logistic Regression (lr 0.2, 20 epochs) **(served)** | 0.817 | 91.5% | 76.1% | 0.831 | 108/142 | 10 | 267/430 (62.1%) |
| Logistic Regression (lr 1.0, 5 epochs) | 0.769 | 83.5% | 74.7% | 0.788 | 106/142 | 21 | 321/430 (74.7%) |
| Logistic Regression, L2 (liblinear, C=30) | 0.852 | 91.8% | 78.9% | 0.849 | 112/142 | 10 | 122/430 (28.4%) |
| Logistic Regression, L2 (liblinear, C=100) | 0.854 | 89.7% | 79.6% | 0.843 | 113/142 | 13 | 143/430 (33.3%) |
| Linear SVM (hinge loss) | 0.755 | 77.7% | 76.1% | 0.769 | 108/142 | 31 | 325/430 (75.6%) |
| Multinomial Naive Bayes | 0.760 | 81.8% | 76.1% | 0.788 | 108/142 | 24 | 247/430 (57.4%) |
| Ensemble: average of Logistic Regression and Naive Bayes | 0.796 | 89.2% | 75.3% | 0.817 | 107/142 | 13 | 267/430 (62.1%) |

What this shows:

- The L2-regularised logistic regression (Tribuo's liblinear trainer) has the best cross-validation F1
  (0.854 vs 0.817) and a slightly better test F1 (0.843-0.849 vs 0.831). On 142 test scams the gap is
  4-5 ads, so it is a small gain.
- On real SMS job scams the same models caught only 28-33% of messages, against 62% for the served
  model. They learn very specific job-ad wording and are confident only on long, formal ads.
- So "best F1 on the main test" was not the same as "best for the people who will use this app", who
  mostly paste short WhatsApp and SMS messages. The served model stays the SGD logistic regression
  (learning rate 0.2, 20 epochs). In the training code the liblinear models are still trained and
  reported every run, but marked as not servable.
- Character n-grams (3-5 characters) were also tried in an early check. Cross-validation F1 went down
  slightly and the number of features grew a lot, so they were dropped.
- Wider vocabulary settings (terms in at least 2 documents, up to 100,000 terms) improved the liblinear
  model a little more (test F1 0.841, 111 of 142 caught, 11 false alarms) but its SMS recall stayed at 32%.

## The whole system, not just the model

The app combines rules, the model and trust checks, so `HybridBenchmarkTest` runs the real analysis
service over both test sets (network lookups stubbed). A message counts as caught at "high risk" (60+)
or at "be careful or higher" (30+).

**Main test, 3,144 job ads (142 fake):**

| | Caught | False alarms |
| --- | --- | --- |
| Model alone | 108 (76.1%) | 10 |
| Full system, high risk | 104 (73.2%) | 5 (precision 95.4%) |
| Full system, be careful or higher | 115 (81.0%) | 32 (1.1% of real ads) |

**Second test, 430 real job-scam SMS:**

| | Served model | L2 model (C=100) |
| --- | --- | --- |
| Model alone | 267 (62.1%) | 138 (32.1%) |
| Full system, high risk | 205 (47.7%) | 57 (13.3%) |
| Full system, be careful or higher | 358 (83.3%) | 143 (33.3%) |

With the served model, the full system warns about 83% of real SMS job scams and calls about half of
them high risk.

## Rule update

The red-flag rules were written for Indian job posts and missed common wording in the SMS set, for
example pay per day in other currencies ("$100 and $300 a day", "5000BRL per day"), "daily salary",
"Whats-App" with a hyphen and "add me on WhatsApp". To change rules without fooling ourselves, the 430
messages were split in half with a fixed seed: rules were written while looking only at the first half,
then measured once on the second half.

| Second half, 215 messages (not looked at) | Before | After |
| --- | --- | --- |
| Full system, high risk | 105 (48.8%) | 109 (50.7%) |
| Full system, be careful or higher | 182 (84.7%) | 183 (85.1%) |
| False alarms on the 3,002 real EMSCAD test ads | 5 / 32 | 5 / 32 (no change) |

A small but free gain: no new false alarms on real job ads. (A few rows of the file were seen while
checking the data format before the split, so the second half is close to, but not perfectly, untouched.)

## Ideas that need more data

- A model trained on short messages needs real *legitimate* SMS and WhatsApp job messages too. Training
  on scam-only messages would teach the model "short message = scam".
- The user feedback button ("was this correct?") already stores labelled examples from real use. After
  a few hundred of those, retraining with them is the most promising next step.
- Hindi and Hinglish messages are not covered by EMSCAD at all.

## How to reproduce

```bash
cd backend
./mvnw -Ptrain compile exec:java -Dexec.args="--data ../data/fake_job_postings.csv --out src/main/resources/model --scam-test ../data/imc25_smishing.csv"
./mvnw test -Dtest=HybridBenchmarkTest -Dbenchmark=true \
    -Dbenchmark.data=../data/fake_job_postings.csv -Dbenchmark.sms=../data/imc25_smishing.csv
```

Training is seeded, so the same data gives the same model; the served model files were reproduced byte
for byte by the current training code.
