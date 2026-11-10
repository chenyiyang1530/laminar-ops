import request from '@/utils/request'

export function crudMeta(res) {
  return request.get(`/crud/${res}/meta`)
}
export function crudPage(res, params) {
  return request.get(`/crud/${res}/page`, { params })
}
export function crudGet(res, id) {
  return request.get(`/crud/${res}/${id}`)
}
export function crudOptions(res, params) {
  return request.get(`/crud/${res}/options`, { params })
}
export function crudAdd(res, data) {
  return request.post(`/crud/${res}`, data)
}
export function crudUpdate(res, data) {
  return request.put(`/crud/${res}`, data)
}
export function crudDelete(res, ids) {
  return request.delete(`/crud/${res}/${ids}`)
}
