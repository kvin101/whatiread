/** Product copy — clear, concise. */
export const copy = {
  brand: {
    name: 'WhatIRead',
    tagline: 'Track what you read',
    motto: 'Read more. Share shelves.',
    footnote: 'AGPL-3.0',
  },
  nav: {
    library: 'My Books',
    shelves: 'Shelves',
    explore: 'Explore',
    friends: 'Friends',
    messages: 'Messages',
    recommendations: 'Recommendations',
    settings: 'Account',
    administration: 'Administration',
    profile: 'Profile',
    signOut: 'Sign out',
  },
  auth: {
    login: {
      title: 'Sign in',
      subtitle: 'Welcome back',
      quote: 'Track books, organize shelves, and share recommendations with friends.',
      submit: 'Sign in',
      submitting: 'Signing in…',
      noAccount: 'New here?',
      createAccount: 'Create account',
    },
    register: {
      title: 'Create account',
      subtitle: 'Start tracking your reading',
      submit: 'Create account',
      submitting: 'Creating account…',
      hasAccount: 'Already have an account?',
      signIn: 'Sign in',
    },
    setup: {
      title: 'Set up this instance',
      subtitle:
        'Create the admin account and choose whether new users can register.',
      submit: 'Complete setup',
      registrationHint:
        'When registration is off, create accounts from Administration.',
    },
    registrationClosed: {
      message: 'This instance is not accepting new sign-ups. Contact an administrator.',
    },
    registrationDisabled: {
      message: 'This instance is not accepting new sign-ups. Contact an administrator.',
      adminHint: 'Admins can invite users in Administration.',
    },
  },
  library: {
    title: 'My Books',
    description: (count: number) =>
      count === 0 ? 'No books yet' : `${count} book${count === 1 ? '' : 's'}`,
    addBook: 'Add book',
    searchPlaceholder: 'Search by title, author, or ISBN…',
    discover: 'Shelves to explore',
    seeAll: 'View all',
    empty: {
      title: 'No books yet',
      description: 'Add a book to start building your library.',
      cta: 'Add book',
    },
    noResults: {
      title: 'No results',
      description: 'Try a different search term.',
    },
  },
  shelves: {
    title: 'My Shelves',
    description: 'Organize books into shelves and control who can see them',
    explore: 'Explore shelves',
    newShelf: 'New shelf',
    empty: {
      title: 'No shelves yet',
      description: 'Create a shelf to group books by theme, project, or mood.',
      cta: 'Create shelf',
    },
    create: {
      title: 'New shelf',
      namePlaceholder: 'Shelf name',
      descPlaceholder: 'Optional description',
    },
  },
  explore: {
    title: 'Explore',
    description: 'Public shelves, friends\' shelves, and shelves shared with you',
    empty: {
      title: 'Nothing to explore yet',
      description: 'Add friends, share shelves, or make a shelf public.',
    },
  },
  friends: {
    title: 'Friends',
    inviteLabel: 'Find people',
    invitePlaceholder: 'Search by username or name…',
    send: 'Send request',
    incoming: 'Incoming requests',
    outgoing: 'Sent requests',
    decline: 'Decline',
    cancelRequest: 'Cancel',
    declineConfirm: {
      title: 'Decline request?',
      description: (name: string) => `${name} can send another request later.`,
      confirm: 'Decline',
    },
    cancelConfirm: {
      title: 'Cancel request?',
      description: (name: string) => `Withdraw your request to ${name}.`,
      confirm: 'Cancel request',
    },
    block: 'Block',
    blockConfirm: {
      title: 'Block this user?',
      description: (name: string) =>
        `${name} will be unfriended and cannot message you or send requests.`,
      confirm: 'Block',
    },
    yourFriends: (n: number) => `Friends (${n})`,
    empty: {
      title: 'No friends yet',
      description: 'Search for readers and send a friend request.',
    },
    unfriend: {
      title: 'Unfriend?',
      description: (name: string) => `${name} will be removed from your friends list.`,
      confirm: 'Unfriend',
    },
    blocked: (n: number) => `Blocked (${n})`,
    blockedEmpty: {
      title: 'No blocked users',
      description: 'Blocked users appear here. You can unblock them anytime.',
    },
    unblock: 'Unblock',
  },
  recommendations: {
    title: 'Recommendations',
    recommend: 'Recommend',
    inbox: (n: number) => `Inbox (${n})`,
    suggested: 'Suggested for you',
    emptyInbox: {
      title: 'No recommendations',
      description: 'Books friends recommend to you appear here.',
    },
    sent: (n: number) => `Sent (${n})`,
    emptySent: {
      title: 'No sent recommendations',
      description: 'Books you recommend to friends appear here.',
    },
    emptySuggestions: {
      title: 'No suggestions yet',
      description: 'Read more books and connect with friends to get suggestions.',
    },
    modal: {
      title: 'Recommend books',
      messagePlaceholder: 'Optional note…',
      submit: 'Send',
    },
  },
  messages: {
    title: 'Messages',
    empty: {
      title: 'No conversations',
      description: 'Start a chat with a friend about books.',
    },
    placeholder: 'Type a message…',
    send: 'Send',
  },
  settings: {
    title: 'Account',
    admin: {
      title: 'Administration',
      description: 'Manage users and registration settings',
      manageUsers: 'Manage users',
      registrationOff: 'Public registration is disabled. Create accounts in Administration.',
      registrationOn: 'Anyone can register. Disable to switch to invite-only.',
      registrationLabel: 'Allow public registration',
    },
  },
  admin: {
    title: 'User administration',
    description: 'Create accounts, reset passwords, and manage access',
    searchPlaceholder: 'Search by email or name…',
    createUser: 'Create user',
    empty: {
      title: 'No users found',
      description: 'Try a different search or create an account.',
    },
    columns: {
      user: 'User',
      role: 'Role',
      status: 'Status',
      actions: 'Actions',
    },
    roleAdmin: 'Admin',
    roleUser: 'User',
    statusActive: 'Active',
    statusBanned: 'Banned',
    resetPassword: 'Reset password',
    ban: 'Ban',
    unban: 'Unban',
    delete: 'Delete',
    create: {
      title: 'Create user',
      submit: 'Create account',
    },
    reset: {
      title: 'Reset password',
      description: (email: string) => `Set a new password for ${email}.`,
      submit: 'Update password',
    },
    banConfirm: {
      title: 'Ban this user?',
      description: (name: string) => `${name} will be signed out and unable to log in.`,
      confirm: 'Ban user',
    },
    unbanConfirm: {
      title: 'Restore access?',
      description: (name: string) => `${name} will be able to sign in again.`,
      confirm: 'Unban user',
    },
    deleteConfirm: {
      title: 'Delete this user?',
      description: (name: string) => `${name} will be removed. This cannot be undone.`,
      confirm: 'Delete user',
    },
  },
  profile: {
    addFriend: 'Add friend',
    unfriend: 'Unfriend',
    block: 'Block',
    unblock: 'Unblock',
    blockedBanner: 'You blocked this user. They cannot message you or send friend requests.',
    blockConfirm: (name: string) =>
      `Block ${name}? They will be unfriended and cannot contact you.`,
  },
  confirm: {
    cancel: 'Cancel',
    delete: 'Delete',
    remove: 'Remove',
  },
} as const
