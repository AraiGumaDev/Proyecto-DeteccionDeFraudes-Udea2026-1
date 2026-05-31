const BASE_URL = '/api/v1'

async function request(method, path, body = null, params = null) {
  let url = `${BASE_URL}${path}`
  if (params) {
    const qs = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v !== null && v !== undefined && v !== ''))
    )
    if ([...qs].length) url += `?${qs}`
  }

  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body) opts.body = JSON.stringify(body)

  const res = await fetch(url, opts)
  const data = await res.json().catch(() => null)

  if (!res.ok) {
    const msg = data?.mensaje || data?.detalle || `Error ${res.status}`
    throw new Error(msg)
  }
  return data
}

export const get    = (path, params) => request('GET',    path, null,  params)
export const post   = (path, body)   => request('POST',   path, body)
export const patch  = (path)         => request('PATCH',  path)
export const del    = (path)         => request('DELETE', path)
