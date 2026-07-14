/** Product voice: warm, witty, never corporate. */
export const copy = {
  brand: {
    name: 'WhatIRead',
    tagline: 'Your TBR pile, but with receipts.',
    motto: 'Read more. Judge gently. Share recklessly.',
    footnote: 'AGPL-3.0 · Your instance, your rules',
  },
  nav: {
    library: 'The pile',
    shelves: 'Curated chaos',
    explore: 'Shelf tourism',
    friends: 'Co-conspirators',
    messages: 'Book gossip',
    recommendations: 'Peer pressure',
    settings: 'Knobs & dials',
    administration: 'Administration',
    profile: 'Main character energy',
    signOut: 'Close the book',
  },
  auth: {
    login: {
      title: 'Back for more pages?',
      subtitle: 'Your bookmarks missed you. Probably.',
      quote: 'Track what you read. Steal shelves from friends. Pretend your TBR is under control.',
      submit: 'Let me in',
      submitting: 'Dog-earing the login page…',
      noAccount: 'New around here?',
      createAccount: 'Claim a shelf',
    },
    register: {
      title: 'Join the bookish conspiracy',
      subtitle: 'One account. Infinite guilt about unread books.',
      submit: 'Start my pile',
      submitting: 'Stamping your library card…',
      hasAccount: 'Already hoarding books?',
      signIn: 'Sign in',
    },
    setup: {
      title: 'First one in gets the keys',
      subtitle:
        'Spin up the admin account. Decide if strangers can register — if not, you can invite readers from Administration later.',
      submit: 'Open the doors',
      registrationHint:
        'When registration is off, use Administration (Settings) to create accounts for your readers.',
    },
    registrationClosed: {
      message:
        'This instance isn\'t accepting new sign-ups. Ask an administrator for an account.',
    },
    registrationDisabled: {
      message:
        'This instance isn\'t accepting new sign-ups. Ask an administrator for an account.',
      adminHint: 'Admins can invite users in Administration.',
    },
  },
  library: {
    title: 'Your reading pile',
    description: (count: number) =>
      count === 0
        ? 'Zero books logged. The audacity.'
        : `${count} book${count === 1 ? '' : 's'} — some finished, most "almost there"`,
    addBook: 'Rescue a book',
    searchPlaceholder: 'Hunt by title, author, or that ISBN you photographed once…',
    discover: 'Shelves worth peeking at',
    seeAll: 'More shelf tourism',
    empty: {
      title: "Shelf's bare. Embarrassing.",
      description:
        'Add a book before someone asks what you\'re reading and you panic-blurt "newsletters."',
      cta: 'Add the first victim',
    },
    noResults: {
      title: 'Nothing matched that search',
      description: 'Try fewer words, or admit you made up the title at brunch.',
    },
  },
  shelves: {
    title: 'Curated chaos',
    description: 'Mini-libraries with vibes, visibility settings, and mild drama',
    explore: 'Browse the stacks',
    newShelf: 'Birth a shelf',
    empty: {
      title: 'No shelves yet',
      description: 'Group books like you group personality traits — loosely, with an emoji.',
      cta: 'Create your first shelf',
    },
    create: {
      title: 'New shelf, who dis?',
      namePlaceholder: 'e.g. Books that broke me (in a good way)',
      descPlaceholder: 'Why this pile exists. Future-you will need the context.',
    },
  },
  explore: {
    title: 'Shelf tourism',
    description: 'Public stacks, friend shelves, and the ones someone quietly shared with you',
    empty: {
      title: 'Quiet out here',
      description: 'Befriend readers, get invited to shelves, or publish your own and become famous (locally).',
    },
  },
  friends: {
    title: 'Co-conspirators',
    description: 'People you trust with recs, shelves, and spicy takes on chapter 12',
    inviteLabel: 'Find a reader',
    invitePlaceholder: 'Search by username or name…',
    send: 'Slide into their DMs (politely)',
    incoming: 'Want in',
    outgoing: 'Waiting on them',
    decline: 'Decline',
    cancelRequest: 'Cancel request',
    declineConfirm: {
      title: 'Decline request?',
      description: (name: string) => `${name} won't join your crew — they can request again later.`,
      confirm: 'Decline',
    },
    cancelConfirm: {
      title: 'Cancel request?',
      description: (name: string) => `Withdraw your invite to ${name}. No hard feelings.`,
      confirm: 'Cancel request',
    },
    block: 'Block',
    blockConfirm: {
      title: 'Block this reader?',
      description: (name: string) =>
        `${name} will be unfriended and can't message you or send requests. You can unblock later.`,
      confirm: 'Block',
    },
    yourFriends: (n: number) => `Your crew (${n})`,
    empty: {
      title: 'Flying solo',
      description: 'Send a friend request. Reading is better when you can blame someone for a bad rec.',
    },
    unfriend: {
      title: 'Unfriend?',
      description: (name: string) => `${name} will vanish from your crew. Shelves stay, awkwardness might not.`,
      confirm: 'Yeah, unfriend',
    },
    blocked: (n: number) => `Blocked (${n})`,
    blockedEmpty: {
      title: 'Nobody in the penalty box',
      description: 'Blocked readers show up here. You can unblock them anytime — we don\'t hold grudges.',
    },
    unblock: 'Unblock',
  },
  recommendations: {
    title: 'Peer pressure',
    description: 'Friends telling you what to read next — the wholesome kind',
    recommend: 'Pressure a friend',
    inbox: (n: number) => `Inbox (${n})`,
    suggested: 'Algorithmic nudges',
    emptyInbox: {
      title: 'Inbox zero (for now)',
      description: 'When friends send recs, they land here. No spam, just guilt about your TBR.',
    },
    sent: (n: number) => `Sent (${n})`,
    emptySent: {
      title: 'No recs in flight',
      description: 'Books you recommend to friends show up here until they accept or hard-pass.',
    },
    emptySuggestions: {
      title: 'No nudges yet',
      description: 'Read a few books, make a friend or two, and we\'ll start suggesting things you\'ll ignore politely.',
    },
    modal: {
      title: 'Recommend books',
      messagePlaceholder: 'Read these because… (optional but persuasive)',
      submit: 'Send the rec',
    },
  },
  messages: {
    title: 'Book gossip',
    description: 'DMs about plots, shelves, and who dog-eared what',
    empty: {
      title: 'No chats yet',
      description: 'Pick a friend and start talking books. Plot spoilers optional, strongly discouraged.',
    },
    placeholder: 'Type a message… @mention shelves, books, or people',
    send: 'Send',
  },
  settings: {
    title: 'Knobs & dials',
    description: 'Profile, yearly goals, Goodreads escape hatch, and data export',
    admin: {
      title: 'Administration',
      description: 'Invite readers and manage accounts when public registration is off',
      manageUsers: 'Manage users',
      registrationOff: 'Public registration is disabled. Admins can invite users in Administration.',
      registrationOn: 'Anyone can register. Disable registration to invite-only mode.',
      registrationLabel: 'Allow public registration',
    },
  },
  admin: {
    title: 'User administration',
    description: 'Create accounts, reset passwords, and manage access for this instance',
    searchPlaceholder: 'Search by email or name…',
    createUser: 'Create user',
    empty: {
      title: 'No users found',
      description: 'Try a different search or create the first account.',
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
      description: (email: string) => `Set a new temporary password for ${email}.`,
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
      description: (name: string) => `${name} will be removed from this instance. This cannot be undone.`,
      confirm: 'Delete user',
    },
  },
  profile: {
    addFriend: 'Add to crew',
    unfriend: 'Remove from crew',
    block: 'Block',
    unblock: 'Unblock',
    blockedBanner: 'You blocked this reader. They can\'t message you or send friend requests.',
    blockConfirm: (name: string) =>
      `Block ${name}? They'll be unfriended and can't message you. Nuclear, but fair.`,
  },
  confirm: {
    cancel: 'Never mind',
    delete: 'Delete it',
    remove: 'Remove',
  },
} as const
