import { useCallback, useEffect, useState } from 'react';

const defaultValue = 'ATU-12345678';

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  });

  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.error ?? 'Request failed');
  }
  return body;
}

function Verdict({ secure, vulnerable, children }) {
  const tone = secure ? 'secure' : vulnerable ? 'vulnerable' : 'neutral';
  return <span className={`verdict ${tone}`}>{children}</span>;
}

function Ciphertext({ label, value }) {
  return (
    <div className="ciphertext">
      <span>{label}</span>
      <code>{value || 'Run the exercise to produce a value'}</code>
    </div>
  );
}

function App() {
  const [value, setValue] = useState(defaultValue);
  const [status, setStatus] = useState(null);
  const [weakResult, setWeakResult] = useState(null);
  const [keyResult, setKeyResult] = useState(null);
  const [observations, setObservations] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState('');

  const refresh = useCallback(async () => {
    try {
      const [nextStatus, nextObservations] = await Promise.all([
        api('/api/labs/status'),
        api('/api/labs/observations'),
      ]);
      setStatus(nextStatus);
      setObservations(nextObservations);
      setError('');
    } catch (requestError) {
      setError(requestError.message);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function runWeakLab() {
    setBusy('weak');
    try {
      setWeakResult(await api('/api/labs/weak/encrypt-twice', {
        method: 'POST',
        body: JSON.stringify({ value }),
      }));
      await refresh();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy('');
    }
  }

  async function runKeyLab() {
    setBusy('key');
    try {
      setKeyResult(await api('/api/labs/hardcoded/encrypt', {
        method: 'POST',
        body: JSON.stringify({ value }),
      }));
      await refresh();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy('');
    }
  }

  const weakIsSecure = status?.weakLabAlgorithm === 'AES/GCM/NoPadding';
  const keyIsSecure = status?.hardcodedLabKeySource !== 'Hard-coded constant in source code';

  return (
    <main>
      <header className="hero">
        <div className="eyebrow">LOCAL SECURITY WORKSHOP · OWASP A04</div>
        <h1>Customer Data <span>Crypto Lab</span></h1>
        <p>
          Find three cryptographic failures, change the implementation, and prove each fix with
          CodeQL, SonarQube, ZAP, or Burp Suite Professional.
        </p>
        <div className="warning">
          <strong>Intentionally vulnerable.</strong> Bind it only to localhost and never deploy the
          starting version to a shared or production environment.
        </div>
      </header>

      <section className="status-strip" aria-label="Runtime status">
        <div>
          <span>Application transport</span>
          <strong>{status?.requestScheme?.toUpperCase() ?? 'Checking…'}</strong>
        </div>
        <div>
          <span>Database</span>
          <strong>{status?.database?.startsWith('Oracle') ? 'Oracle connected' : 'Checking…'}</strong>
        </div>
        <div>
          <span>Stored values</span>
          <strong>{observations.length} ciphertext samples</strong>
        </div>
      </section>

      {error && <div className="error" role="alert">{error}</div>}

      <section className="input-panel">
        <label htmlFor="customer-value">Synthetic customer value</label>
        <div className="input-row">
          <input
            id="customer-value"
            value={value}
            maxLength={256}
            onChange={(event) => setValue(event.target.value)}
          />
          <span>Use test data only</span>
        </div>
      </section>

      <section className="lab-grid">
        <article className="lab-card">
          <div className="lab-number">01</div>
          <div className="lab-heading">
            <div>
              <p>Algorithm and mode</p>
              <h2>Weak encryption</h2>
            </div>
            <Verdict secure={weakIsSecure} vulnerable={!weakIsSecure}>
              {weakIsSecure ? 'Fixed' : 'Vulnerable'}
            </Verdict>
          </div>
          <p className="explanation">
            Encrypt the same value twice. ECB produces identical ciphertext; authenticated
            encryption with a fresh nonce should not.
          </p>
          <button onClick={runWeakLab} disabled={!value || busy === 'weak'}>
            {busy === 'weak' ? 'Encrypting…' : 'Encrypt twice'}
          </button>
          <div className="result-block">
            <Ciphertext label="First result" value={weakResult?.firstCiphertext} />
            <Ciphertext label="Second result" value={weakResult?.secondCiphertext} />
            {weakResult && (
              <p className={weakResult.identical ? 'bad-result' : 'good-result'}>
                {weakResult.identical
                  ? 'Finding reproduced: the ciphertexts are identical.'
                  : 'Fix verified at runtime: the ciphertexts differ.'}
              </p>
            )}
          </div>
          <footer>
            <code>WeakCryptoService.java</code>
            <span>Verify with CodeQL or SonarQube</span>
          </footer>
        </article>

        <article className="lab-card">
          <div className="lab-number">02</div>
          <div className="lab-heading">
            <div>
              <p>Key management</p>
              <h2>Hard-coded key</h2>
            </div>
            <Verdict secure={keyIsSecure} vulnerable={!keyIsSecure}>
              {keyIsSecure ? 'Fixed' : 'Vulnerable'}
            </Verdict>
          </div>
          <p className="explanation">
            The cipher is sound, but its key is committed with the code and will be copied into
            every Fargate container image.
          </p>
          <button onClick={runKeyLab} disabled={!value || busy === 'key'}>
            {busy === 'key' ? 'Encrypting…' : 'Encrypt with configured key'}
          </button>
          <div className="result-block">
            <Ciphertext label="Stored in Oracle" value={keyResult?.ciphertext} />
            <p className={keyIsSecure ? 'good-result' : 'bad-result'}>
              Key source: {status?.hardcodedLabKeySource ?? 'Checking…'}
            </p>
          </div>
          <footer>
            <code>HardCodedKeyService.java</code>
            <span>Verify with SonarQube or CodeQL</span>
          </footer>
        </article>

        <article className="lab-card transport-card">
          <div className="lab-number">03</div>
          <div className="lab-heading">
            <div>
              <p>Data in transit</p>
              <h2>Cleartext HTTP</h2>
            </div>
            <Verdict secure={status?.secureTransport} vulnerable={!status?.secureTransport}>
              {status?.secureTransport ? 'Fixed' : 'Vulnerable'}
            </Verdict>
          </div>
          <p className="explanation">
            The browser is connected over <strong>{status?.requestScheme ?? '…'}</strong>. On HTTP,
            an observer can read or change customer values before encryption occurs.
          </p>
          <div className="transport-diagram">
            <span>Browser</span><i>→</i><span>CloudFront / edge</span><i>→</i><span>ECS API</span>
          </div>
          <div className="result-block">
            <p className={status?.secureTransport ? 'good-result' : 'bad-result'}>
              {status?.secureTransport
                ? 'HTTPS is active. Confirm HSTS with ZAP and verify the certificate in Burp.'
                : 'Finding reproduced: requests and responses use unencrypted HTTP.'}
            </p>
          </div>
          <footer>
            <code>application.yml</code>
            <span>Verify with ZAP or Burp Suite Professional</span>
          </footer>
        </article>
      </section>

      <section className="history">
        <div>
          <p className="section-kicker">ORACLE DATABASE</p>
          <h2>Latest encrypted observations</h2>
        </div>
        <button className="secondary" onClick={refresh}>Refresh</button>
        <div className="history-table">
          {observations.length === 0 ? (
            <p>No observations yet. Run one of the encryption exercises.</p>
          ) : observations.map((observation) => (
            <div className="history-row" key={observation.id}>
              <span>#{observation.id}</span>
              <strong>{observation.labName}</strong>
              <code>{observation.algorithm}</code>
              <code>{observation.ciphertext.slice(0, 34)}…</code>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}

export default App;

