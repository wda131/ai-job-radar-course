export const salaryK = value => {
  if (value == null) return '?'
  const amount = value >= 1000 ? value / 1000 : value
  return Number.isInteger(amount) ? amount : amount.toFixed(1)
}

export const salaryRange = job => {
  if (job.salaryText?.trim()) return job.salaryText.trim()
  if (!job.salaryMin && !job.salaryMax) return '薪资面议'
  return `${salaryK(job.salaryMin)}–${salaryK(job.salaryMax)}K`
}
