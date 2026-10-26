import type { ReactNode } from 'react'
import { IonContent, IonPage } from '@ionic/react'

interface Props {
  title: string
  subtitle: string
  children: ReactNode
}

export default function AuthLayout({ title, subtitle, children }: Props) {
  return (
    <IonPage>
      <IonContent className="app-content">
        <div style={{ maxWidth: 420, margin: '0 auto', paddingTop: '8vh' }}>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <div style={{ fontSize: 56, lineHeight: 1 }} aria-hidden="true">
              🥗
            </div>
            <h1 style={{ margin: '12px 0 4px', fontWeight: 800 }}>{title}</h1>
            <p className="muted" style={{ margin: 0 }}>
              {subtitle}
            </p>
          </div>
          <div className="card">{children}</div>
        </div>
      </IonContent>
    </IonPage>
  )
}
