import { useMutation, useQueryClient } from '@tanstack/react-query'
import { friendsApi } from '../api/friends'

export function useFriendsMutations() {
  const queryClient = useQueryClient()

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['friends'] })
  }

  const sendMutation = useMutation({
    mutationFn: (userId: string) => friendsApi.sendRequest({ userId }),
    onSuccess: invalidate,
  })

  const acceptMutation = useMutation({
    mutationFn: (requestId: string) => friendsApi.accept(requestId),
    onSuccess: invalidate,
  })

  const declineMutation = useMutation({
    mutationFn: (requestId: string) => friendsApi.decline(requestId),
    onSuccess: invalidate,
  })

  const cancelMutation = useMutation({
    mutationFn: (requestId: string) => friendsApi.cancel(requestId),
    onSuccess: invalidate,
  })

  const unfriendMutation = useMutation({
    mutationFn: (friendUserId: string) => friendsApi.unfriend(friendUserId),
    onSuccess: invalidate,
  })

  const blockMutation = useMutation({
    mutationFn: (userId: string) => friendsApi.block(userId),
    onSuccess: invalidate,
  })

  const unblockMutation = useMutation({
    mutationFn: (userId: string) => friendsApi.unblock(userId),
    onSuccess: invalidate,
  })

  return {
    invalidate,
    sendMutation,
    acceptMutation,
    declineMutation,
    cancelMutation,
    unfriendMutation,
    blockMutation,
    unblockMutation,
    isPending:
      sendMutation.isPending ||
      acceptMutation.isPending ||
      declineMutation.isPending ||
      cancelMutation.isPending ||
      unfriendMutation.isPending ||
      blockMutation.isPending ||
      unblockMutation.isPending,
  }
}
