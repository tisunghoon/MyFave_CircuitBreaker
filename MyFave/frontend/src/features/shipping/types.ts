export interface Address {
  id: string
  name: string
  isDefault: boolean
  address: string
  detailAddress?: string
  zipcode?: string
  request?: string
  phone: string
}

export interface TrackingEvent {
  statusCode: string
  statusName: string
  time: string
  location: string
  description: string
}

export interface TrackingResponse {
  trackingNumber: string
  carrierId: string
  statusCode: string
  statusName: string
  time: string
  location: string
  description: string
  events: TrackingEvent[]
}
