import React, { useState, useEffect, useCallback } from 'react'
import { Button } from '@portal/design-react'
import { sellerApi } from '@/api'

const ProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(true)

  const fetchProfile = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await sellerApi.getProfile()
      setProfile(response.data?.data || null)
    } catch (err) {
      console.error('Failed to load profile:', err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchProfile()
  }, [fetchProfile])

  if (isLoading) {
    return <div className="p-12 text-center text-text-meta">Loading profile...</div>
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Seller Profile</h1>
      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm max-w-2xl">
        {profile ? (
          <div className="space-y-4">
            <div>
              <label className="text-sm text-text-meta">Business Name</label>
              <p className="text-text-heading font-medium">{profile.businessName || '-'}</p>
            </div>
            <div>
              <label className="text-sm text-text-meta">Business Number</label>
              <p className="text-text-heading">{profile.businessNumber || '-'}</p>
            </div>
            <div>
              <label className="text-sm text-text-meta">Representative</label>
              <p className="text-text-heading">{profile.representativeName || '-'}</p>
            </div>
            <div>
              <label className="text-sm text-text-meta">Commission Rate</label>
              <p className="text-text-heading">{profile.commissionRate || '10.00'}%</p>
            </div>
            <div>
              <label className="text-sm text-text-meta">Status</label>
              <p className="text-text-heading">{profile.status || '-'}</p>
            </div>
          </div>
        ) : (
          <div className="text-center">
            <p className="text-text-heading mb-4">No seller profile found.</p>
            <Button variant="primary">Register as Seller</Button>
          </div>
        )}
      </div>
    </div>
  )
}

export default ProfilePage
