export const jobActionState = (jobId, favoriteIds, applicationJobIds) => {
  const favorited = favoriteIds.has(jobId)
  const applied = applicationJobIds.has(jobId)
  return {
    favorited,
    applied,
    favoriteLabel: favorited ? '已收藏' : '收藏',
    applyLabel: applied ? '已投递' : '投递'
  }
}

export const chooseInterviewSessionId = (sessions, requestedId) => {
  const requested = requestedId == null ? '' : String(requestedId)
  const match = sessions.find(session => String(session.id) === requested)
  return match?.id ?? sessions[0]?.id ?? null
}
