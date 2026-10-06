import assert from 'node:assert/strict';
import http from 'node:http';

// This receiver exists only on the isolated test network; no Slack URL is used.
const messages = [];
const receiver = http.createServer(async (request, response) => {
  if (request.url === '/actuator/health' || request.url === '/actuator/prometheus') {
    response.end('UP');
    return;
  }
  let body = '';
  for await (const chunk of request) {
    body += chunk;
  }
  messages.push(JSON.parse(body));
  response.end('ok');
});
await new Promise(resolve => receiver.listen(8080, '0.0.0.0', resolve));

async function waitFor(predicate, label) {
  const deadline = Date.now() + 45000;
  while (Date.now() < deadline) {
    if (await predicate()) {
      return;
    }
    await new Promise(resolve => setTimeout(resolve, 250));
  }
  throw new Error(`Timed out: ${label}`);
}

async function post(path, body) {
  const response = await fetch(`http://alertmanager:9093/api/v2/${path}`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(body),
  });
  assert.equal(response.ok, true, `${path}: ${response.status}`);
  return response;
}

function alerts(name, endsAt) {
  return ['one', 'two'].map(instance => ({
    labels: {alertname: name, service: 'my-fitness', environment: 'prod', instance},
    annotations: {summary: `Test ${instance}`, description: 'Local receiver only'},
    startsAt: new Date(Date.now() - 10000).toISOString(),
    endsAt,
  }));
}

try {
  await waitFor(async () => {
    try {
      return (await fetch('http://alertmanager:9093/-/ready')).ok
        && (await fetch('http://caddy:8081/actuator/health')).ok;
    } catch {
      return false;
    }
  }, 'test services ready');

  for (const path of ['/actuator/prometheus', '/actuator/info', '/actuator']) {
    const response = await fetch(`http://caddy:8081${path}`, {
      headers: {Authorization: `Basic ${Buffer.from('prometheus:dummy-metrics').toString('base64')}`},
    });
    assert.equal(response.status, 404, `public ${path}`);
  }
  assert.equal((await fetch('http://receiver:8080/actuator/prometheus')).status, 200);

  await post('alerts', alerts('GroupedProbe', new Date(Date.now() + 60000).toISOString()));
  await waitFor(() => messages.length === 1, 'grouped firing');
  assert.match(messages[0].attachments[0].title, /FIRING.*GroupedProbe.*2/);
  assert.match(messages[0].attachments[0].text, /Test one/);
  assert.match(messages[0].attachments[0].text, /Test two/);

  await post('alerts', alerts('GroupedProbe', new Date().toISOString()));
  await waitFor(() => messages.length === 2, 'resolved notification');
  assert.match(messages[1].attachments[0].title, /RESOLVED.*GroupedProbe.*2/);

  const silence = await post('silences', {
    matchers: [{name: 'alertname', value: 'SilencedProbe', isRegex: false, isEqual: true}],
    startsAt: new Date().toISOString(),
    endsAt: new Date(Date.now() + 60000).toISOString(),
    createdBy: 'local-test', comment: 'No external delivery',
  });
  assert.ok((await silence.json()).silenceID);
  await post('alerts', alerts('SilencedProbe', new Date(Date.now() + 60000).toISOString()));
  // Observe longer than group_wait so silence, rather than waiting, suppresses delivery.
  await new Promise(resolve => setTimeout(resolve, 35000));
  assert.equal(messages.length, 2, 'silenced alerts must not be delivered');
  console.log('Caddy isolation, grouped firing, resolved and silence passed');
} finally {
  receiver.close();
}
