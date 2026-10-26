import { IonItem, IonLabel, IonSkeletonText } from '@ionic/react'

export default function ListSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div className="row-list" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <IonItem key={i} lines="none">
          <IonLabel>
            <h3>
              <IonSkeletonText animated style={{ width: '55%' }} />
            </h3>
            <p>
              <IonSkeletonText animated style={{ width: '30%' }} />
            </p>
          </IonLabel>
        </IonItem>
      ))}
    </div>
  )
}
