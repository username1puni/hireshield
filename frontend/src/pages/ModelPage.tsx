import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { Metrics } from '../api/types'
import { percent } from '../utils/format'

export default function ModelPage() {
  const [metrics, setMetrics] = useState<Metrics | null>(null)

  useEffect(() => {
    api.metrics().then(setMetrics).catch(() => setMetrics(null))
  }, [])

  const cm = metrics?.test.confusionMatrix

  return (
    <div className="container page">
      <h1>How HireShield works</h1>
      <div className="card prose">
        <p>Every check runs three independent parts and then combines them:</p>
        <ol>
          <li><strong>Red-flag rules.</strong> Twelve checks based on Indian cyber-crime advisories: asking for a fee, task-based earning, asking for OTP or bank details, free email IDs, WhatsApp-only contact, pay that is too good to be true, pressure to act fast and more. Each rule shows the exact words it matched.</li>
          <li><strong>Machine learning.</strong> A logistic regression model (Tribuo, in Java) trained on the EMSCAD dataset of real and fake job ads, using TF-IDF word and word-pair features.</li>
          <li><strong>Trust checks.</strong> How old the company website is (RDAP), whether its domain can receive email, whether it imitates a known company, risky links, and whether any email, phone or UPI ID was already reported by users.</li>
        </ol>
        <p>The three parts are combined so that one strong signal is enough to raise the score: 0-29 is low risk, 30-59 means be careful, 60 and above is high risk.</p>
      </div>

      {metrics && cm && (
        <>
          <h2>Model results on unseen data</h2>
          <p className="muted">
            Measured on {metrics.dataset.testSize} job ads ({metrics.dataset.testFraudulent} fake) that the model never saw during
            training. Duplicates were removed before splitting.
          </p>
          <div className="metric-grid">
            <div className="card metric"><strong>{percent(metrics.test.precision)}</strong><span>Precision</span><small>Of the ads we flagged, how many were really fake</small></div>
            <div className="card metric"><strong>{percent(metrics.test.recall)}</strong><span>Recall</span><small>Of all fake ads, how many we caught</small></div>
            <div className="card metric"><strong>{metrics.test.f1.toFixed(3)}</strong><span>F1 score</span><small>Balance of precision and recall</small></div>
            <div className="card metric"><strong>{metrics.test.rocAuc.toFixed(3)}</strong><span>ROC-AUC</span><small>Ranking quality, 1.0 is perfect</small></div>
          </div>

          <div className="card">
            <h3>Confusion matrix</h3>
            <table className="confusion">
              <thead><tr><th></th><th>Predicted fake</th><th>Predicted real</th></tr></thead>
              <tbody>
                <tr><th>Actually fake</th><td className="cm-good">{cm.truePositive}</td><td className="cm-bad">{cm.falseNegative}</td></tr>
                <tr><th>Actually real</th><td className="cm-bad">{cm.falsePositive}</td><td className="cm-good">{cm.trueNegative}</td></tr>
              </tbody>
            </table>
            <p className="muted tiny">
              Why not just accuracy? A model that always says "real" would get {percent(metrics.baselineAlwaysReal.accuracy)} accuracy
              on this data and catch zero scams. That is why we report precision and recall for the fake class.
            </p>
          </div>

          {metrics.secondTest && (
            <div className="card prose">
              <h3>Second test: real SMS and WhatsApp job scams</h3>
              <p>
                Job ads are not the only way scams arrive, so the model was also tested on {metrics.secondTest.rows} real
                job-scam messages reported by users (IMC 2025 smishing dataset, CC BY 4.0). None of them were used for
                training. The model alone flagged <strong>{metrics.secondTest.selectedModel.caught}</strong> of them
                ({percent(metrics.secondTest.selectedModel.recall)}). The red-flag rules and trust checks catch many of the rest.
              </p>
            </div>
          )}

          {metrics.crossValidation && (
            <div className="card table-wrap">
              <h3>Models compared (5-fold cross validation)</h3>
              <table className="checks">
                <thead><tr><th>Model</th><th>Precision</th><th>Recall</th><th>F1</th><th>SMS scams caught</th></tr></thead>
                <tbody>
                  {metrics.crossValidation.map((row) => {
                    const sms = metrics.secondTest?.allModels?.[row.model]
                    return (
                      <tr key={row.model} className={row.model === metrics.algorithm ? 'selected-row' : undefined}>
                        <td>{row.model}{row.model === metrics.algorithm ? ' (used)' : ''}</td>
                        <td>{percent(row.precision)}</td><td>{percent(row.recall)}</td><td>{row.f1.toFixed(3)}</td>
                        <td>{sms ? percent(sms.recall) : '-'}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              <p className="tiny muted">
                The L2 (liblinear) models score a bit higher on job ads, but they caught less than half as many of the real SMS
                scams as the model in use, so the app keeps the model that holds up better on both.
              </p>
              <p className="tiny muted">Model version {metrics.version}, decision threshold {metrics.threshold}.</p>
            </div>
          )}
        </>
      )}

      <div className="card prose">
        <h3>Limits</h3>
        <p>
          The dataset is made of English job ads from 2012-2014, so the model alone is weaker on short WhatsApp messages and
          Hinglish. That is why the rules and trust checks carry a lot of weight. A low score is not a guarantee: always verify
          the company on its official website before sharing documents or money.
        </p>
      </div>
    </div>
  )
}
