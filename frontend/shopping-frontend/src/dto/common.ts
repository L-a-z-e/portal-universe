export interface Address {
  receiverName: string
  receiverPhone: string
  zipCode: string
  address1: string
  address2?: string
}

export interface AddressRequest {
  receiverName: string
  receiverPhone: string
  zipCode: string
  address1: string
  address2?: string
}
