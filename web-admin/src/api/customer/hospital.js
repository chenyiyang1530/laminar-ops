import request from '@/utils/request'

export function pageHospital(params) {
  return request.get('/customer/hospital/page', { params })
}

export function getHospital(id) {
  return request.get('/customer/hospital/' + id)
}

export function addHospital(data) {
  return request.post('/customer/hospital', data)
}

export function updateHospital(data) {
  return request.put('/customer/hospital', data)
}

export function delHospital(ids) {
  return request.delete('/customer/hospital/' + ids)
}

export function hospitalOptions() {
  return request.get('/customer/hospital/options')
}

export function regionOptions() {
  return request.get('/customer/region/options')
}
