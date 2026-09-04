import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge, PriorityBadge } from './Badges'

describe('StatusBadge', () => {
  it.each([
    ['OPEN', 'Open'],
    ['IN_PROGRESS', 'In Progress'],
    ['RESOLVED', 'Resolved'],
    ['CLOSED', 'Closed'],
  ] as const)('renders label for %s', (status, label) => {
    render(<StatusBadge status={status} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })
})

describe('PriorityBadge', () => {
  it.each([
    ['LOW', 'Low'],
    ['NORMAL', 'Normal'],
    ['HIGH', 'High'],
    ['URGENT', 'Urgent'],
  ] as const)('renders label for %s', (priority, label) => {
    render(<PriorityBadge priority={priority} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })
})
