import { request } from '../request';

export interface SessionListParams {
  page?: number;
  size?: number;
  status?: string;
}

export interface SessionRangeParams {
  startTime: string;
  endTime: string;
  page?: number;
  size?: number;
}

export function fetchCreateSession() {
  return request<Api.Session.CreateResponse>({
    url: '/sessions',
    method: 'post'
  });
}

export function fetchSessionList(params: SessionListParams = {}) {
  return request<Api.Session.ListResponse | Api.Session.Item[]>({
    url: '/sessions',
    params
  });
}

export function fetchSessionListByRange(params: SessionRangeParams) {
  return request<Api.Session.ListResponse | Api.Session.Item[]>({
    url: '/sessions/range',
    params
  });
}

export function fetchSessionDetail(id: string) {
  return request<Api.Session.DetailResponse | Api.Chat.Message[]>({
    url: `/sessions/${id}/messages`
  });
}

export function fetchDeleteSession(id: string) {
  return request<boolean>({
    url: `/sessions/${id}`,
    method: 'delete'
  });
}

export function fetchUpdateSessionTitle(id: string, title: string) {
  return request<boolean>({
    url: `/sessions/${id}/title`,
    method: 'put',
    data: { title }
  });
}

export function fetchAutoUpdateSessionTitle(id: string) {
  return request<boolean>({
    url: `/sessions/${id}/auto-title`,
    method: 'put'
  });
}

export function fetchArchiveSession(id: string, archived = true) {
  return request<boolean>({
    url: `/sessions/${id}/archive`,
    method: 'put',
    data: { archived }
  });
}

export function fetchActiveSessionCount() {
  return request<Api.Session.CountResponse | number>({
    url: '/sessions/count'
  });
}
