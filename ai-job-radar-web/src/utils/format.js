export const salaryK = value => {
  if (value == null) return '?'
  const amount = value >= 1000 ? value / 1000 : value
  return Number.isInteger(amount) ? amount : amount.toFixed(1)
}

export const salaryRange = job => `${salaryK(job.salaryMin)}–${salaryK(job.salaryMax)}K`
