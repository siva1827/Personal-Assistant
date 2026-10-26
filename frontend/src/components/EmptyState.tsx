import type { ReactNode } from 'react'

interface Props {
  emoji: string
  title: string
  children?: ReactNode
}

export default function EmptyState({ emoji, title, children }: Props) {
  return (
    <div className="empty-state">
      <div className="emoji" aria-hidden="true">
        {emoji}
      </div>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
    </div>
  )
}
